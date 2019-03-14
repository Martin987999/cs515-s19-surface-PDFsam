/*
 * This file is part of the PDF Split And Merge source code
 * Created on 26/giu/2014
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.alternatemix;

import org.pdfsam.support.params.AbstractPdfOutputParametersBuilder;
import org.pdfsam.support.params.SingleOutputTaskParametersBuilder;
import org.pdfsam.task.AlternateMixSingleInputParameters;
import org.sejda.common.collection.NullSafeSet;
import org.sejda.model.input.PdfMixInput;
import org.sejda.model.output.FileTaskOutput;
import org.sejda.model.parameter.AlternateMixMultipleInputParameters;

import java.util.Set;

/**
 * Builder for the {@link AlternateMixMultipleInputParameters}
 *
 * @author Andrea Vacondio
 */
class AlternateMixParametersBuilder extends AbstractPdfOutputParametersBuilder<AlternateMixSingleInputParameters>
        implements SingleOutputTaskParametersBuilder<AlternateMixSingleInputParameters> {

    private FileTaskOutput output;
    private Set<PdfMixInput> inputs = new NullSafeSet<>();

    @Override
    public void output(FileTaskOutput output) {
        this.output = output;
    }

    void addInput(PdfMixInput input) {
        this.inputs.add(input);
    }

    boolean hasInput() {
        return !inputs.isEmpty();
    }

    @Override
    public AlternateMixSingleInputParameters build() {
        AlternateMixSingleInputParameters params = new AlternateMixSingleInputParameters();
        params.setCompress(isCompress());
        params.setExistingOutputPolicy(existingOutput());
        params.setVersion(getVersion());
        params.setOutput(output);
        inputs.forEach(params::addInput);
        return params;
    }
}
