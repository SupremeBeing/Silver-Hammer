/*
 * Copyright (c) 2017, Dmitriy Shchekotin
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

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.*;

import ru.silverhammer.control.IControl;
import ru.silverhammer.control.IValueListener;

// TODO: consider adding control renderers
public abstract class Control<Value, A extends Annotation, C extends JComponent> extends JPanel implements IControl<Value, A> {

	protected abstract class SearchAdapter extends KeyAdapter {
		
		private long accessTime;
		private String search = "";
		
		@Override
		public final void keyTyped(KeyEvent e) {
			char ch = e.getKeyChar();
			if (accessTime + 1000 < System.currentTimeMillis()) {
				search = "";
			}
			accessTime = System.currentTimeMillis();
			search += ch;

			search(search);
		}
		
		protected abstract void search(String search);
	}

	private static final long serialVersionUID = 6368631261236160508L;

	private final C component;
	private final boolean scrollable;
	private final Collection<IValueListener> listeners = new ArrayList<>();

	private Color normalBackground;
	private Color invalidBackground = Color.RED;

	protected Control(boolean scrollable) {
		this.component = createComponent();
		this.scrollable = scrollable;
		setLayout(new BorderLayout());
		if (scrollable) {
			JScrollPane scroll = new JScrollPane(component);
			add(scroll, BorderLayout.CENTER);
		} else {
			add(component, BorderLayout.CENTER);
		}
		normalBackground = component.getBackground();
	}

	protected Color getInvalidBackground() {
		return invalidBackground;
	}

	protected void setNormalBackground(Color normalBackground) {
		this.normalBackground = normalBackground;
	}

	protected Color getNormalBackground() {
		return normalBackground;
	}

	@Override
	public void setValidationMessage(String message) {
		setValidationMessage(component, message);
	}

	protected void setValidationMessage(JComponent component, String message) {
		component.setToolTipText(message);
		component.setBackground(message == null ? normalBackground : invalidBackground);
	}

	@Override
	public String getValidationMessage() {
		return component.getToolTipText();
	}

	@Override
	public boolean isControlValid() {
		return getValidationMessage() == null;
	}


	@Override
	public boolean isEnabled() {
		return component.isEnabled();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		component.setEnabled(enabled);
	}

	@Override
	public Dimension getMinimumSize() {
		if (scrollable) {
			return getPreferredSize();
		} else {
			return super.getMinimumSize();
		}
	}

	protected abstract C createComponent();
	
	protected C getComponent() {
		return component;
	}
	
	@Override
	public void addValueListener(IValueListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeValueListener(IValueListener listener) {
		listeners.remove(listener);
	}
	
	protected void fireValueChanged() {
		for (IValueListener l : listeners) {
			l.changed(this);
		}
	}
}
