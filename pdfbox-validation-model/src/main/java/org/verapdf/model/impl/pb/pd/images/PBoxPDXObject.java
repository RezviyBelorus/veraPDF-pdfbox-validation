/**
 * This file is part of veraPDF PDF Box PDF/A Validation Model Implementation, a module of the veraPDF project.
 * Copyright (c) 2015, veraPDF Consortium <info@verapdf.org>
 * All rights reserved.
 *
 * veraPDF PDF Box PDF/A Validation Model Implementation is free software: you can redistribute it and/or modify
 * it under the terms of either:
 *
 * The GNU General public license GPLv3+.
 * You should have received a copy of the GNU General Public License
 * along with veraPDF PDF Box PDF/A Validation Model Implementation as the LICENSE.GPL file in the root of the source
 * tree.  If not, see http://www.gnu.org/licenses/ or
 * https://www.gnu.org/licenses/gpl-3.0.en.html.
 *
 * The Mozilla Public License MPLv2+.
 * You should have received a copy of the Mozilla Public License along with
 * veraPDF PDF Box PDF/A Validation Model Implementation as the LICENSE.MPL file in the root of the source tree.
 * If a copy of the MPL was not distributed with this file, you can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.verapdf.model.impl.pb.pd.images;

import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.graphics.PDPostScriptXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObjectProxy;
import org.verapdf.model.baselayer.Object;
import org.verapdf.model.coslayer.CosDict;
import org.verapdf.model.impl.pb.cos.PBCosDict;
import org.verapdf.model.impl.pb.pd.PBoxPDResources;
import org.verapdf.model.pdlayer.PDSMaskImage;
import org.verapdf.model.pdlayer.PDXObject;
import org.verapdf.model.tools.resources.PDInheritableResources;
import org.verapdf.pdfa.flavours.PDFAFlavour;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Evgeniy Muravitskiy
 */
public class PBoxPDXObject extends PBoxPDResources implements PDXObject {

	private static final Logger LOGGER = Logger.getLogger(PBoxPDXObject.class);

	protected final PDDocument document;
	protected final PDFAFlavour flavour;

	public static final String X_OBJECT_TYPE = "PDXObject";

	public static final String OPI = "OPI";
	public static final String S_MASK = "SMask";

	protected final PDInheritableResources resources;
	private final String subtype;

	public PBoxPDXObject(org.apache.pdfbox.pdmodel.graphics.PDXObject simplePDObject, PDDocument document,
			PDFAFlavour flavour) {
		this(simplePDObject, PDInheritableResources.EMPTY_EXTENDED_RESOURCES, X_OBJECT_TYPE, document, flavour);
	}

	protected PBoxPDXObject(COSObjectable simplePDObject, PDInheritableResources resources, final String type,
			PDDocument document, PDFAFlavour flavour) {
		super(simplePDObject, type);
		this.resources = resources;
		this.subtype = PBoxPDXObject.getSubtype((org.apache.pdfbox.pdmodel.graphics.PDXObject) this.simplePDObject);
		this.document = document;
		this.flavour = flavour;
	}

	private static String getSubtype(org.apache.pdfbox.pdmodel.graphics.PDXObject object) {
		COSBase base = object.getCOSStream().getDictionaryObject(COSName.SUBTYPE);
		return base instanceof COSName ? ((COSName) base).getName() : null;
	}

	@Override
	public String getSubtype() {
		return this.subtype;
	}

	@Override
	public Boolean getcontainsOPI() {
		COSBase pageObject = this.simplePDObject.getCOSObject();
		return pageObject != null && pageObject instanceof COSDictionary &&
				((COSDictionary) pageObject).containsKey(COSName.getPDFName("OPI"));
	}

	@Override
	public Boolean getcontainsSMask() {
		COSBase pageObject = this.simplePDObject.getCOSObject();
		return pageObject != null && pageObject instanceof COSDictionary &&
				((COSDictionary) pageObject).containsKey(COSName.SMASK);
	}


	@Override
	public List<? extends Object> getLinkedObjects(String link) {
		switch (link) {
		case S_MASK:
			return this.getSMask();
		default:
			return super.getLinkedObjects(link);
		}
	}

	protected List<PDSMaskImage> getSMask() {
		try {
			COSStream cosStream = ((org.apache.pdfbox.pdmodel.graphics.PDXObject) this.simplePDObject).getCOSStream();
			COSBase smaskDictionary = cosStream.getDictionaryObject(COSName.SMASK);
			if (smaskDictionary instanceof COSDictionary) {
				PDSMaskImage xObject = this.getXObject(smaskDictionary);
				if (xObject != null) {
					List<PDSMaskImage> mask = new ArrayList<>(MAX_NUMBER_OF_ELEMENTS);
					mask.add(xObject);
					return Collections.unmodifiableList(mask);
				}
			}
		} catch (IOException e) {
			LOGGER.debug("Problems with obtaining SMask. " + e.getMessage(), e);
		}
		return Collections.emptyList();
	}

	private PDSMaskImage getXObject(COSBase smaskDictionary) throws IOException {
		COSName name = ((COSDictionary) smaskDictionary).getCOSName(COSName.NAME);
		String nameAsString = name != null ? name.getName() : null;
		PDResources resourcesLocal = null;
		if (this.simplePDObject instanceof PDFormXObject) {
			resourcesLocal = ((PDFormXObject) this.simplePDObject).getResources();
		}
		org.apache.pdfbox.pdmodel.graphics.PDXObject pbObject = org.apache.pdfbox.pdmodel.graphics.PDXObject
				.createXObject(smaskDictionary, nameAsString, resourcesLocal);
		if (pbObject instanceof PDImageXObjectProxy) {
			return new PBoxPDSMaskImage((PDImageXObjectProxy) pbObject, resources, document, flavour);
		}
		LOGGER.debug("SMask object is not an Image XObject");
		return null;
	}

	public static PDXObject getTypedPDXObject(org.apache.pdfbox.pdmodel.graphics.PDXObject pbObject,
			PDInheritableResources extendedResources, PDDocument document, PDFAFlavour flavour) {
		if (pbObject instanceof PDFormXObject) {
			PDFormXObject object = (PDFormXObject) pbObject;
			PDInheritableResources resources = extendedResources.getExtendedResources(object.getResources());
			return new PBoxPDXForm(object, resources, document, flavour);
		} else if (pbObject instanceof PDImageXObjectProxy) {
			return new PBoxPDXImage((PDImageXObjectProxy) pbObject, extendedResources,
					document, flavour);
		} else if (pbObject instanceof PDPostScriptXObject) {
			return new PBoxPDXObject(pbObject, document, flavour);
		} else {
			return null;
		}
	}

	protected List<CosDict> getLinkToDictionary(String key) {
		COSDictionary object = ((org.apache.pdfbox.pdmodel.graphics.PDXObject) this.simplePDObject).getCOSStream();
		COSBase item = object.getDictionaryObject(COSName.getPDFName(key));
		if (item instanceof COSDictionary) {
			List<CosDict> list = new ArrayList<>(MAX_NUMBER_OF_ELEMENTS);
			list.add(new PBCosDict((COSDictionary) item, this.document, this.flavour));
			return Collections.unmodifiableList(list);
		}
		return Collections.emptyList();
	}
}
