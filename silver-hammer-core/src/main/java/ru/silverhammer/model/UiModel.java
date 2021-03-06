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
package ru.silverhammer.model;

import ru.junkie.IInjector;
import ru.reflexio.*;
import ru.sanatio.Validation;
import ru.sanatio.ValidationResult;
import ru.sanatio.conversion.IStringConverter;
import ru.sanatio.validator.ValidatorReference;
import ru.silverhammer.converter.ConverterReference;
import ru.silverhammer.converter.IConverter;
import ru.silverhammer.control.IControl;
import ru.silverhammer.processor.ControlProcessor;
import ru.silverhammer.processor.ProcessorReference;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class UiModel {

    private final List<CategoryModel> categories = new ArrayList<>();
    private final List<MethodModel> initializers = new ArrayList<>();
    private final List<MethodModel> validators = new ArrayList<>();

    private final IInjector injector;
    private final Validation validation;

    public UiModel(IInjector injector, IStringConverter converter) {
        this.injector = injector;
        this.validation = new Validation(converter);
    }

    public List<CategoryModel> getCategories() {
        return categories;
    }

    public List<MethodModel> getInitializers() {
        return initializers;
    }

    public List<MethodModel> getValidators() {
        return validators;
    }

    public GroupModel findGroupModel(String groupId) {
        for (CategoryModel c : categories) {
            for (GroupModel g : c.getGroups()) {
                if (Objects.equals(g.getId(), groupId)) {
                    return g;
                }
            }
        }
        return null;
    }

    public CategoryModel findCategoryModel(String caption) {
        for (CategoryModel c : categories) {
            if (Objects.equals(c.getCaption(), caption)) {
                return c;
            }
        }
        return null;
    }

    public void visitControlModels(Consumer<ControlModel> consumer) {
        for (CategoryModel c : categories) {
            for (GroupModel ga : c.getGroups()) {
                for (ControlModel ca : ga.getControls()) {
                    consumer.accept(ca);
                }
            }
        }
    }

    public ControlModel findControlModel(Predicate<ControlModel> predicate) {
        for (CategoryModel c : categories) {
            for (GroupModel ga : c.getGroups()) {
                for (ControlModel ca : ga.getControls()) {
                    if (predicate.test(ca)) {
                        return ca;
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends IControl<?, ?>> T findControl(Object data, Class<?> type, String fieldName) {
        IFieldReflection field = new TypeReflection<>(type).findInstanceField(fieldName);
        if (field != null) {
            ControlModel attrs = findControlModel(ca -> ca.getFieldReflection().equals(field) && ca.getData().equals(data));
            if (attrs != null) {
                return (T) attrs.getControl();
            }
        }
        return null;
    }

    public <T extends IControl<?, ?>> T findControl(Object data, String fieldName) {
        return findControl(data, data.getClass(), fieldName);
    }

    public void commit() {
		visitControlModels(c -> commit(c.getData(), c.getFieldReflection(), c.getControl()));
	}

	private void commit(Object data, IInstanceFieldReflection field, IControl<?, ?> control) {
		Object value = getFieldValue(control.getValue(), field);
		field.setValue(data, value);
	}

	public boolean isValid() {
		return findControlModel(ca -> !ca.getControl().isControlValid()) == null;
	}

	public void initialize() {
		initializeMethods();
		visitControlModels(ca -> init(ca.getControl(), ca.getFieldReflection()));
		validateMethods();
	}

	private void init(IControl<?, ?> control, IFieldReflection field) {
		validateControl(control, field);
		control.addValueListener(c -> {
			validateControl(control, field);
			validateMethods();
		});
	}

	// TODO: consider exposing full data validation method
	private void validateControl(IControl<?, ?> control, IFieldReflection field) {
		Object value = control.getValue();
		ValidationResult result = new ValidationResult();
		validateValue(value, field, result);
        StringBuilder builder = new StringBuilder();
        for (String message : result) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(message);
        }
		control.setValidationMessage(builder.length() == 0 ? null : builder.toString());
	}

    private void validateValue(Object value, IFieldReflection field, ValidationResult result) {
        for (Annotation annotation : field.getAnnotations()) {
            for (Annotation metaAnnotation : annotation.annotationType().getAnnotations()) {
                if (metaAnnotation instanceof ConverterReference) {
                    ConverterReference cr = (ConverterReference) metaAnnotation;
                    @SuppressWarnings("unchecked")
                    IConverter<Object, Object, Annotation> converter = (IConverter<Object, Object, Annotation>) injector.instantiate(cr.value());
                    value = converter.convertBackward(value, annotation);
                } else if (metaAnnotation instanceof ValidatorReference) {
                    validation.validate(value, (ValidatorReference) metaAnnotation, annotation, result);
                    if (!result.isValid()) {
                        return;
                    }
                }
            }
        }
    }

    private void validateMethods() {
		for (MethodModel ma : validators) {
			injector.invoke(ma.getData(), ma.getMethodReflection());
		}
	}

	private void initializeMethods() {
		for (MethodModel ma : initializers) {
			injector.invoke(ma.getData(), ma.getMethodReflection());
		}
	}

    public Object getControlValue(Object value, IFieldReflection field) {
        List<MetaAnnotation<ConverterReference>> marked = field.getMetaAnnotations(ConverterReference.class);
        for (int i = marked.size() - 1; i >= 0; i--) {
            MetaAnnotation<ConverterReference> ma = marked.get(i);
            @SuppressWarnings("unchecked")
            IConverter<Object, Object, Annotation> converter = (IConverter<Object, Object, Annotation>) injector.instantiate(ma.getMetaAnnotation().value());
            value = converter.convertForward(value, ma.getAnnotation());
        }
        return value;
    }

    public Object getFieldValue(Object value, IFieldReflection field) {
        List<MetaAnnotation<ConverterReference>> marked = field.getMetaAnnotations(ConverterReference.class);
        for (MetaAnnotation<ConverterReference> ma : marked) {
            @SuppressWarnings("unchecked")
            IConverter<Object, Object, Annotation> converter = (IConverter<Object, Object, Annotation>) injector.instantiate(ma.getMetaAnnotation().value());
            value = converter.convertBackward(value, ma.getAnnotation());
        }
        return value;
    }

    public boolean hasControlAnnotation(IFieldReflection fieldReflection) {
        for (MetaAnnotation<ProcessorReference> m : fieldReflection.getMetaAnnotations(ProcessorReference.class)) {
            if (m.getMetaAnnotation().value() == ControlProcessor.class) {
                return true;
            }
        }
        return false;
    }
}
