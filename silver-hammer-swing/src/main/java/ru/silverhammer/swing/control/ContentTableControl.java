/*
 * Copyright (c) 2019, Dmitriy Shchekotin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package ru.silverhammer.swing.control;

import ru.reflexio.IInstanceFieldReflection;
import ru.reflexio.TypeReflection;
import ru.sanatio.conversion.IStringConverter;
import ru.silverhammer.model.UiModel;
import ru.silverhammer.processor.Caption;
import ru.silverhammer.control.ContentTable;

import java.util.Collection;

public class ContentTableControl extends TableControl<ContentTable> {

	private static final long serialVersionUID = -3692427066762483919L;

	private final IStringConverter converter;
	private final UiModel model;

	public ContentTableControl(IStringConverter converter, UiModel model) {
		this.converter = converter;
		this.model = model;
	}
	
	@Override
	public Object getValue() {
		return data.clone();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setValue(Object value) {
		data.clear();
		if (value instanceof Collection) {
			data.addAll((Collection<Object[]>) value);
		}
		getModel().fireTableStructureChanged();
		fireValueChanged();
	}
	
	@Override
	public void init(ContentTable annotation) {
		if (annotation.visibleRows() > 0) {
			setVisibleRowCount(annotation.visibleRows());
		}
		setSelectionType(annotation.multiSelection());
		if (annotation.annotationCaptions() != Void.class) {
			for (IInstanceFieldReflection fr : new TypeReflection<>(annotation.annotationCaptions()).getInstanceFields()) {
				if (model.hasControlAnnotation(fr)) {
					Caption c = fr.getAnnotation(Caption.class);
					getCaptions().add(c == null ? fr.getName() : converter.getString(c.value()));
				}
			}
		} else if (annotation.captions().length > 0) {
			for (String caption : annotation.captions()) {
				getCaptions().add(converter.getString(caption));
			}
		}
	}
}
