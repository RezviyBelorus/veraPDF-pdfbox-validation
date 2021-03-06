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
package org.verapdf.model.impl.pb.operator.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSNumber;
import org.verapdf.model.GenericModelObject;
import org.verapdf.model.coslayer.CosNumber;
import org.verapdf.model.coslayer.CosReal;
import org.verapdf.model.impl.pb.cos.PBCosNumber;
import org.verapdf.model.impl.pb.cos.PBCosReal;
import org.verapdf.model.operator.Operator;

/**
 * Base class for operator layer
 * 
 * @author Timur Kamalov
 */
public abstract class PBOperator extends GenericModelObject implements Operator {

    public static final int MAX_NUMBER_OF_ELEMENTS = 1;
    protected final List<COSBase> arguments;

    protected PBOperator(List<COSBase> arguments, final String opType) {
		super(opType);
        this.arguments = arguments;
    }

    protected List<CosNumber> getLastNumber() {
		if (!this.arguments.isEmpty()) {
			COSBase base = this.arguments.get(this.arguments.size() - 1);
			if (base instanceof COSNumber) {
				List<CosNumber> cosNumbers = new ArrayList<>(MAX_NUMBER_OF_ELEMENTS);
				cosNumbers.add(PBCosNumber.fromPDFBoxNumber(base));
				return Collections.unmodifiableList(cosNumbers);
			}
		}
        return Collections.emptyList();
    }

    protected List<CosReal> getLastReal() {
		if (!this.arguments.isEmpty()) {
			COSBase base = this.arguments.get(this.arguments.size() - 1);
			if (base instanceof COSFloat) {
				List<CosReal> cosReals = new ArrayList<>(MAX_NUMBER_OF_ELEMENTS);
				cosReals.add(new PBCosReal((COSFloat) base));
				return Collections.unmodifiableList(cosReals);
			}
		}
        return Collections.emptyList();
    }

	protected List<CosNumber> getListOfNumbers() {
		List<CosNumber> list = new ArrayList<>();
		for (COSBase base : this.arguments) {
			if (base instanceof COSArray) {
				addArrayElementsAsNumbers(list, (COSArray) base);
			} else if (base instanceof COSNumber) {
				list.add(PBCosNumber.fromPDFBoxNumber(base));
			}
		}
		return Collections.unmodifiableList(list);
	}

	protected List<CosReal> getListOfReals() {
		List<CosReal> list = new ArrayList<>();
		for (COSBase base : this.arguments) {
			if (base instanceof COSArray) {
				addArrayElementsAsReals(list, (COSArray) base);
			} else if (base instanceof COSFloat) {
				list.add(new PBCosReal((COSFloat) base));
			}
		}
		return Collections.unmodifiableList(list);
	}

	private static void addArrayElementsAsReals(List<CosReal> list, COSArray base) {
		for (COSBase arg : base) {
			if (arg instanceof COSFloat) {
				list.add(new PBCosReal((COSFloat) arg));
			}
		}
	}

	private static void addArrayElementsAsNumbers(List<CosNumber> list, COSArray base) {
		for (COSBase arg : base) {
			list.add(PBCosNumber.fromPDFBoxNumber(arg));
		}
	}

}
