package org.eclipse.swt.widgets;

/*
 * Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
 
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.events.*;

/**
 * Instances of this class implement the notebook user interface
 * metaphor.  It allows the user to select a notebook page from
 * set of pages.
 * <p>
 * The item children that may be added to instances of this class
 * must be of type <code>TabItem</code>.
 * <code>Control</code> children are created and then set into a
 * tab item using <code>TabItem#setControl</code>.
 * </p><p>
 * Note that although this class is a subclass of <code>Composite</code>,
 * it does not make sense to set a layout on it.
 * </p><p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>(none)</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Selection</dd>
 * </dl>
 * <p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 */
public class TabFolder extends Composite {
	TabItem [] items;
	ImageList imageList;
	static final int TabFolderProc;
	static final TCHAR TabFolderClass = new TCHAR (0, "SWT_" + OS.WC_TABCONTROL, true);
	static {
		/*
		* Feature in Windows.  The tab control window class
		* uses the CS_HREDRAW and CS_VREDRAW style bits to
		* force a full redraw of the control and all children
		* when resized.  This causes flashing.  The fix is to
		* register a new window class without these bits and
		* implement special code that damages only the exposed
		* area.
		*/
		WNDCLASS lpWndClass = new WNDCLASS ();
		TCHAR WC_TABCONTROL = new TCHAR (0, OS.WC_TABCONTROL, true);
		OS.GetClassInfo (0, WC_TABCONTROL, lpWndClass);
		TabFolderProc = lpWndClass.lpfnWndProc;
		int hInstance = OS.GetModuleHandle (null);
		if (!OS.GetClassInfo (hInstance, TabFolderClass, lpWndClass)) {
			int hHeap = OS.GetProcessHeap ();
			lpWndClass.hInstance = hInstance;
			lpWndClass.style &= ~(OS.CS_HREDRAW | OS.CS_VREDRAW);
			int byteCount = TabFolderClass.length () * TCHAR.sizeof;
			int lpszClassName = OS.HeapAlloc (hHeap, OS.HEAP_ZERO_MEMORY, byteCount);
			OS.MoveMemory (lpszClassName, TabFolderClass, byteCount);
			lpWndClass.lpszClassName = lpszClassName;
			OS.RegisterClass (lpWndClass);
//			OS.HeapFree (hHeap, 0, lpszClassName);
		}
	}

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a composite control which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see SWT
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public TabFolder (Composite parent, int style) {
	super (parent, checkStyle (style));
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the receiver's selection changes, by sending
 * it one of the messages defined in the <code>SelectionListener</code>
 * interface.
 * <p>
 * When <code>widgetSelected</code> is called, the item field of the event object is valid.
 * <code>widgetDefaultSelected</code> is not called.
 * </p>
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SelectionListener
 * @see #removeSelectionListener
 * @see SelectionEvent
 */
public void addSelectionListener(SelectionListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener(listener);
	addListener(SWT.Selection,typedListener);
	addListener(SWT.DefaultSelection,typedListener);
}

int callWindowProc (int msg, int wParam, int lParam) {
	if (handle == 0) return 0;
	return OS.CallWindowProc (TabFolderProc, handle, msg, wParam, lParam);
}

static int checkStyle (int style) {
	/*
	* Even though it is legal to create this widget
	* with scroll bars, they serve no useful purpose
	* because they do not automatically scroll the
	* widget's client area.  The fix is to clear
	* the SWT style.
	*/
	return style & ~(SWT.H_SCROLL | SWT.V_SCROLL);
}

protected void checkSubclass () {
	if (!isValidSubclass ()) error (SWT.ERROR_INVALID_SUBCLASS);
}

public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget ();
	RECT insetRect = new RECT (), itemRect = new RECT ();
	OS.SendMessage (handle, OS.TCM_ADJUSTRECT, 0, insetRect);
	int width = insetRect.left - insetRect.right, height = 0;
	int count = OS.SendMessage (handle, OS.TCM_GETITEMCOUNT, 0, 0);
	if (count != 0) {
		OS.SendMessage (handle, OS.TCM_GETITEMRECT, count - 1, itemRect);
		width = Math.max (width, itemRect.right - insetRect.right);
	}
	Point size;
	if (layout != null) {
		size = layout.computeSize (this, wHint, hHint, changed);
	} else {
		size = minimumSize ();
	}
	if (size.x == 0) size.x = DEFAULT_WIDTH;
	if (size.y == 0) size.y = DEFAULT_HEIGHT;
	if (wHint != SWT.DEFAULT) size.x = wHint;
	if (hHint != SWT.DEFAULT) size.y = hHint;
	width = Math.max (width, size.x);
	height = Math.max (height, size.y);
	Rectangle trim = computeTrim (0, 0, width, height);
	width = trim.width;  height = trim.height;
	return new Point (width, height);
}

public Rectangle computeTrim (int x, int y, int width, int height) {
	checkWidget ();
	RECT rect = new RECT ();
	OS.SetRect (rect, x, y, x + width, y + height);
	OS.SendMessage (handle, OS.TCM_ADJUSTRECT, 1, rect);
	int border = getBorderWidth ();
	rect.left -= border;  rect.right += border;
	rect.top -= border;  rect.bottom += border;
	int newWidth = rect.right - rect.left;
	int newHeight = rect.bottom - rect.top;
	return new Rectangle (rect.left, rect.top, newWidth, newHeight);
}

void createItem (TabItem item, int index) {
	int count = OS.SendMessage (handle, OS.TCM_GETITEMCOUNT, 0, 0);
	if (!(0 <= index && index <= count)) error (SWT.ERROR_INVALID_RANGE);
	if (count == items.length) {
		TabItem [] newItems = new TabItem [items.length + 4];
		System.arraycopy (items, 0, newItems, 0, items.length);
		items = newItems;
	}
	TCITEM tcItem = new TCITEM ();
	if (OS.SendMessage (handle, OS.TCM_INSERTITEM, index, tcItem) == -1) {
		error (SWT.ERROR_ITEM_NOT_ADDED);
	}
	System.arraycopy (items, index, items, index + 1, count - index);
	items [index] = item;
	
	/*
	* Send a selection event when the item that is added becomes
	* the new selection.  This only happens when the first item
	* is added.
	*/
	if (count == 0) {
		Event event = new Event ();
		event.item = items [0];
		sendEvent (SWT.Selection, event);
		// the widget could be destroyed at this point
	}
}

void createHandle () {
	super.createHandle ();
	state &= ~CANVAS;
	
	/*
	* Feature in Windows.  Despite the fact that the
	* tool tip text contains \r\n, the tooltip will
	* not honour the new line unless TTM_SETMAXTIPWIDTH
	* is set.  The fix is to set TTM_SETMAXTIPWIDTH to
	* a large value.
	*/
	int hwndToolTip = OS.SendMessage (handle, OS.TCM_GETTOOLTIPS, 0, 0);
	OS.SendMessage (hwndToolTip, OS.TTM_SETMAXTIPWIDTH, 0, 0x7FFF);
	
	/*
	* Feature in Windows.  When the tool tip control is
	* created, the parent of the tool tip is the shell.
	* If SetParent () is used to reparent the tab folder
	* into a new shell, the tool tip is not reparented
	* and pops up underneath the new shell.  The fix is
	* to make sure the tool tip is a topmost window.
	*/
	/*
	* Bug in Windows 98 and NT.  Setting the tool tip to be the
	* top most window using HWND_TOPMOST can result in a parent
	* dialog shell being moved behind its parent if the dialog
	* has a sibling that is currently on top.  The fix is to lock
	* the z-order of the active window.
	*/
	Display display = getDisplay ();
	int flags = OS.SWP_NOACTIVATE | OS.SWP_NOMOVE | OS.SWP_NOSIZE;
	display.lockActiveWindow = true;
	OS.SetWindowPos (hwndToolTip, OS.HWND_TOPMOST, 0, 0, 0, 0, flags);
	display.lockActiveWindow = false;
}

void createWidget () {
	super.createWidget ();
	items = new TabItem [4];
}

void destroyItem (TabItem item) {
	int count = OS.SendMessage (handle, OS.TCM_GETITEMCOUNT, 0, 0);
	int index = 0;
	while (index < count) {
		if (items [index] == item) break;
		index++;
	}
	if (index == count) return;
	int selectionIndex = OS.SendMessage (handle, OS.TCM_GETCURSEL, 0, 0);
	if (OS.SendMessage (handle, OS.TCM_DELETEITEM, index, 0) == 0) {
		error (SWT.ERROR_ITEM_NOT_REMOVED);
	}
	System.arraycopy (items, index + 1, items, index, --count - index);
	items [count] = null;
	if (count == 0) {
		if (imageList != null) {
			OS.SendMessage (handle, OS.TCM_SETIMAGELIST, 0, 0);
			Display display = getDisplay ();
			display.releaseImageList (imageList);
		}
		imageList = null;
		items = new TabItem [4];
	}
	if (count > 0 && index == selectionIndex) {
		setSelection (Math.max (0, selectionIndex - 1));
		selectionIndex = getSelectionIndex ();
		if (selectionIndex != -1) {
			Event event = new Event ();
			event.item = items [selectionIndex];
			sendEvent (SWT.Selection, event);
			// the widget could be destroyed at this point
		}
	}
}

public Rectangle getClientArea () {
	checkWidget ();
	if (parent.hdwp != 0) {
		int oldHdwp = parent.hdwp;
		parent.hdwp = 0;
		OS.EndDeferWindowPos (oldHdwp);
		int count = parent.getChildrenCount ();
		parent.hdwp = OS.BeginDeferWindowPos (count);
	}
	RECT rect = new RECT ();
	OS.GetClientRect (handle, rect);
	OS.SendMessage (handle, OS.TCM_ADJUSTRECT, 0, rect);
	int width = rect.right - rect.left;
	int height = rect.bottom - rect.top;
	return new Rectangle (rect.left, rect.top, width, height);
}

/**
 * Returns the item at the given, zero-relative index in the
 * receiver. Throws an exception if the index is out of range.
 *
 * @param index the index of the item to return
 * @return the item at the given index
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public TabItem getItem (int index) {
	checkWidget ();
	int count = OS.SendMessage (handle, OS.TCM_GETITEMCOUNT, 0, 0);
	if (!(0 <= index && index < count)) error (SWT.ERROR_INVALID_RANGE);
	return items [index];
}

/**
 * Returns the number of items contained in the receiver.
 *
 * @return the number of items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getItemCount () {
	checkWidget ();
	return OS.SendMessage (handle, OS.TCM_GETITEMCOUNT, 0, 0);
}

/**
 * Returns an array of <code>TabItem</code>s which are the items
 * in the receiver. 
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of items, so modifying the array will
 * not affect the receiver. 
 * </p>
 *
 * @return the items in the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public TabItem [] getItems () {
	checkWidget ();
	int count = OS.SendMessage (handle, OS.TCM_GETITEMCOUNT, 0, 0);
	TabItem [] result = new TabItem [count];
	System.arraycopy (items, 0, result, 0, count);
	return result;
}

/**
 * Returns an array of <code>TabItem</code>s that are currently
 * selected in the receiver. An empty array indicates that no
 * items are selected.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its selection, so modifying the array will
 * not affect the receiver. 
 * </p>
 * @return an array representing the selection
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public TabItem [] getSelection () {
	checkWidget ();
	int index = OS.SendMessage (handle, OS.TCM_GETCURSEL, 0, 0);
	if (index == -1) return new TabItem [0];
	return new TabItem [] {items [index]};
}

/**
 * Returns the zero-relative index of the item which is currently
 * selected in the receiver, or -1 if no item is selected.
 *
 * @return the index of the selected item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getSelectionIndex () {
	checkWidget ();
	return OS.SendMessage (handle, OS.TCM_GETCURSEL, 0, 0);
}

int imageIndex (Image image) {
	if (image == null) return OS.I_IMAGENONE;
	if (imageList == null) {
		Rectangle bounds = image.getBounds ();
		imageList = getDisplay ().getImageList (new Point (bounds.width, bounds.height));
		int index = imageList.indexOf (image);
		if (index == -1) index = imageList.add (image);
		int hImageList = imageList.getHandle ();
		OS.SendMessage (handle, OS.TCM_SETIMAGELIST, 0, hImageList);
		return index;
	}
	int index = imageList.indexOf (image);
	if (index != -1) return index;
	return imageList.add (image);
}

/**
 * Searches the receiver's list starting at the first item
 * (index 0) until an item is found that is equal to the 
 * argument, and returns the index of that item. If no item
 * is found, returns -1.
 *
 * @param item the search item
 * @return the index of the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int indexOf (TabItem item) {
	checkWidget ();
	if (item == null) error (SWT.ERROR_NULL_ARGUMENT);
	int count = OS.SendMessage (handle, OS.TCM_GETITEMCOUNT, 0, 0);
	for (int i=0; i<count; i++) {
		if (items [i] == item) return i;
	}
	return -1;
}

boolean mnemonicHit (char key) {
	for (int i=0; i<items.length; i++) {
		TabItem item = items [i];
		if (item != null) {
			char ch = findMnemonic (item.getText ());
			if (Character.toUpperCase (key) == Character.toUpperCase (ch)) {		
				if (setFocus ()) {
					setSelection (i, true);
					return true;
				}
			}
		}
	}
	return false;
}

boolean mnemonicMatch (char key) {
	for (int i=0; i<items.length; i++) {
		TabItem item = items [i];
		if (item != null) {
			char ch = findMnemonic (item.getText ());
			if (Character.toUpperCase (key) == Character.toUpperCase (ch)) {		
				return true;
			}
		}
	}
	return false;
}

void releaseWidget () {
	int count = OS.SendMessage (handle, OS.TCM_GETITEMCOUNT, 0, 0);
	for (int i=0; i<count; i++) {
		TabItem item = items [i];
		if (!item.isDisposed ()) item.releaseResources ();
	}
	items = null;
	if (imageList != null) {
		OS.SendMessage (handle, OS.TCM_SETIMAGELIST, 0, 0);
		Display display = getDisplay ();
		display.releaseImageList (imageList);
	}
	imageList = null;
	super.releaseWidget ();
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the receiver's selection changes.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SelectionListener
 * @see #addSelectionListener
 */
public void removeSelectionListener (SelectionListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Selection, listener);
	eventTable.unhook (SWT.DefaultSelection,listener);	
}

/**
 * Sets the receiver's selection to be the given array of items.
 * The current selected is first cleared, then the new items are
 * selected.
 *
 * @param items the array of items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setSelection (TabItem [] items) {
	checkWidget ();
	if (items == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (items.length == 0) {
		setSelection (-1);
		return;
	}
	for (int i=items.length-1; i>=0; --i) {
		int index = indexOf (items [i]);
		if (index != -1) setSelection (index);
	}
}

/**
 * Selects the item at the given zero-relative index in the receiver. 
 * If the item at the index was already selected, it remains selected.
 * The current selected is first cleared, then the new items are
 * selected. Indices that are out of range are ignored.
 *
 * @param index the index of the item to select
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setSelection (int index) {
	checkWidget ();
	setSelection (index, false);
}

void setSelection (int index, boolean notify) {
	int oldIndex = OS.SendMessage (handle, OS.TCM_GETCURSEL, 0, 0);
	if (oldIndex != -1) {
		TabItem item = items [oldIndex];
		Control control = item.control;
		if (control != null && !control.isDisposed ()) {
			control.setVisible (false);
		}
	}
	OS.SendMessage (handle, OS.TCM_SETCURSEL, index, 0);
	int newIndex = OS.SendMessage (handle, OS.TCM_GETCURSEL, 0, 0);
	if (newIndex != -1) {
		TabItem item = items [newIndex];
		Control control = item.control;
		if (control != null && !control.isDisposed ()) {
			control.setBounds (getClientArea ());
			control.setVisible (true);
		}
		if (notify) {
			Event event = new Event ();
			event.item = item;
			sendEvent (SWT.Selection, event);
		}
	}
}

int toolTipHandle () {
	return OS.SendMessage (handle, OS.TCM_GETTOOLTIPS, 0, 0);
}

String toolTipText (NMTTDISPINFO hdr) {
	if ((hdr.uFlags & OS.TTF_IDISHWND) != 0) {
		return null;
	}
	int index = hdr.idFrom;
	int hwndToolTip = toolTipHandle ();
	if (hwndToolTip == hdr.hwndFrom) {
		if (0 <= index && index < items.length) {
			TabItem item = items [index];
			if (item != null) return item.toolTipText;
		}
	}
	return super.toolTipText (hdr);
}

boolean traversePage (boolean next) {
	int count = getItemCount ();
	if (count == 0) return false;
	int index = getSelectionIndex ();
	if (index == -1) {
		index = 0;
	} else {
		int offset = (next) ? 1 : -1;
		index = (index + offset + count) % count;
	}
	setSelection (index, true);
	return index == getSelectionIndex ();
}

int widgetStyle () {
	/*
	* Bug in Windows.  Under certain circumstances,
	* when TCM_SETITEM is used to change the text
	* in a tab item, the tab folder draws on top
	* of the client area.  The fix is ensure that
	* this cannot happen by setting WS_CLIPCHILDREN.
	*/
	int bits = super.widgetStyle () | OS.WS_CLIPCHILDREN;
	if ((style & SWT.NO_FOCUS) != 0) bits |= OS.TCS_FOCUSNEVER;
	return bits | OS.TCS_TABS | OS.TCS_TOOLTIPS;
}

TCHAR windowClass () {
	return TabFolderClass;
}

int windowProc () {
	return TabFolderProc;
}

LRESULT WM_GETDLGCODE (int wParam, int lParam) {
	LRESULT result = super.WM_GETDLGCODE (wParam, lParam);
	/*
	* Return DLGC_BUTTON so that mnemonics will be
	* processed without needing to press the ALT key
	* when the widget has focus.
	*/
	if (result != null) return result;
	return new LRESULT (OS.DLGC_BUTTON);
}

LRESULT WM_NCHITTEST (int wParam, int lParam) {
	LRESULT result = super.WM_NCHITTEST (wParam, lParam);
	if (result != null) return result;
	/*
	* Feature in Windows.  The tab control implements
	* WM_NCHITTEST to return HTCLIENT when the cursor
	* is inside the tab buttons.  This causes mouse
	* events like WM_MOUSEMOVE to be delivered to the
	* parent.  Also, tool tips for the tab control are
	* never invoked because tool tips rely on mouse
	* events to be delivered to the window that wants
	* to display the tool tip.  The fix is to call the
	* default window proc that returns HTCLIENT when
	* the mouse is in the client area.	
	*/
	int hittest = OS.DefWindowProc (handle, OS.WM_NCHITTEST, wParam, lParam);
	return new LRESULT (hittest);
}

LRESULT WM_NOTIFY (int wParam, int lParam) {
	/*
	* Feature in Windows.  When the tab folder window
	* proc processes WM_NOTIFY, it forwards this
	* message to its parent.  This is done so that
	* children of this control that send this message 
	* type to their parent will notify not only
	* this control but also the parent of this control,
	* which is typically the application window and
	* the window that is looking for the message.
	* If the control did not forward the message, 
	* applications would have to subclass the control 
	* window to see the message. Because the control
	* window is subclassed by SWT, the message
	* is delivered twice, once by SWT and once when
	* the message is forwarded by the window proc.
	* The fix is to avoid calling the window proc 
	* for this control.
	*/
	LRESULT result = super.WM_NOTIFY (wParam, lParam);
	if (result != null) return result;
	return LRESULT.ZERO;
}

LRESULT WM_SIZE (int wParam, int lParam) {
	LRESULT result = super.WM_SIZE (wParam, lParam);
	/*
	* It is possible (but unlikely), that application
	* code could have disposed the widget in the resize
	* event.  If this happens, end the processing of the
	* Windows message by returning the result of the
	* WM_SIZE message.
	*/
	if (isDisposed ()) return result;
	int index = OS.SendMessage (handle, OS.TCM_GETCURSEL, 0, 0);
	if (index != -1) {
		TabItem item = items [index];
		Control control = item.control;
		if (control != null && !control.isDisposed ()) {
			control.setBounds (getClientArea ());
		}
	}
	return result;
}

LRESULT WM_WINDOWPOSCHANGING (int wParam, int lParam) {
	LRESULT result = super.WM_WINDOWPOSCHANGING (wParam, lParam);
	if (result != null) return result;
	if (!OS.IsWindowVisible (handle)) return result;
	WINDOWPOS lpwp = new WINDOWPOS ();
	OS.MoveMemory (lpwp, lParam, WINDOWPOS.sizeof);
	if ((lpwp.flags & (OS.SWP_NOSIZE | OS.SWP_NOREDRAW)) != 0) {
		return result;
	}
	int bits = OS.GetWindowLong (handle, OS.GWL_STYLE);
	if ((bits & OS.TCS_MULTILINE) != 0) {
		OS.InvalidateRect (handle, null, true);
		return result;
	}
	RECT rect = new RECT ();
	OS.SetRect (rect, 0, 0, lpwp.cx, lpwp.cy);
	OS.SendMessage (handle, OS.WM_NCCALCSIZE, 0, rect);
	int newWidth = rect.right - rect.left;
	int newHeight = rect.bottom - rect.top;
	OS.GetClientRect (handle, rect);
	int oldWidth = rect.right - rect.left;
	int oldHeight = rect.bottom - rect.top;
	if (newWidth == oldWidth && newHeight == oldHeight) {
		return result;
	}
	RECT inset = new RECT ();
	OS.SendMessage (handle, OS.TCM_ADJUSTRECT, 0, inset);
	int marginX = -inset.right, marginY = -inset.bottom;
	if (newWidth != oldWidth) {
		int left = oldWidth;
		if (newWidth < oldWidth) left = newWidth;
		OS.SetRect (rect, left - marginX, 0, newWidth, newHeight);
		OS.InvalidateRect (handle, rect, true);
	}
	if (newHeight != oldHeight) {
		int bottom = oldHeight;
		if (newHeight < oldHeight) bottom = newHeight;
		if (newWidth < oldWidth) oldWidth -= marginX;
		OS.SetRect (rect, 0, bottom - marginY, oldWidth, newHeight);
		OS.InvalidateRect (handle, rect, true);
	}
	return result;
}

LRESULT wmNotifyChild (int wParam, int lParam) {
	NMHDR hdr = new NMHDR ();
	OS.MoveMemory (hdr, lParam, NMHDR.sizeof);
	int code = hdr.code;
	switch (code) {
		case OS.TCN_SELCHANGE: 
		case OS.TCN_SELCHANGING:
			TabItem item = null;
			int index = OS.SendMessage (handle, OS.TCM_GETCURSEL, 0, 0);
			if (index != -1) item = items [index];
			if (item != null) {
				Control control = item.control;
				if (control != null && !control.isDisposed ()) {
					if (code == OS.TCN_SELCHANGE) {
						control.setBounds (getClientArea ());
					}
					control.setVisible (code == OS.TCN_SELCHANGE);
				}
			}
			if (code == OS.TCN_SELCHANGE) {
				Event event = new Event ();
				event.item = item;
				postEvent (SWT.Selection, event);
			}
	}
	return super.wmNotifyChild (wParam, lParam);
}

}
