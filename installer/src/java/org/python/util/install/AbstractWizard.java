package org.python.util.install;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public abstract class AbstractWizard extends JDialog implements ValidationListener {

    private class WizardGlassPane extends JPanel implements MouseListener, KeyListener {
        WizardGlassPane() {
            super();
            setOpaque(false);
            addMouseListener(this);
            addKeyListener(this);
        }

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
        }

        public void keyTyped(KeyEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
    }

    private AbstractWizardPage _activePage = null;
    private JPanel _buttonPanel;
    private JSeparator _buttonSeparator;
    private Action _cancelAction;
    private JButton _cancelButton;
    private CardLayout _cards;
    private JPanel _content;
    private WizardGlassPane _glassPane;
    private AbstractWizardHeader _header;
    private boolean _headerVisible = false;
    private ArrayList _listeners;
    private Action _nextAction;
    private JButton _nextButton;
    private ArrayList _pages;
    private Action _previousAction;
    private JButton _previousButton;

    public AbstractWizard() {
        super();
        initWindow();
        initActions();
        initComponents();
    }

    public AbstractWizard(Dialog parent) throws HeadlessException {
        super(parent);
        initWindow();
        initActions();
        initComponents();
    }

    public AbstractWizard(Frame parent) throws HeadlessException {
        super(parent);
        initWindow();
        initActions();
        initComponents();
    }

    public final void addPage(AbstractWizardPage page) {
        if (_pages == null)
            _pages = new ArrayList();
        if (page == null || _pages.contains(page))
            return;
        _pages.add(page);
        page.setWizard(this);
        int number = _pages.indexOf(page);
        _content.add(page, "page" + number);
    }

    public final void addWizardListener(WizardListener listener) {
        if (listener == null)
            return;
        if (_listeners == null)
            _listeners = new ArrayList(5);
        if (_listeners.contains(listener))
            return;
        _listeners.add(listener);
    }

    private void cancel() {
        fireCancelEvent();
        setVisible(false);
    }

    /**
     * @return whether the wizard finished succesfully
     */
    protected abstract boolean finish();

    private void fireCancelEvent() {
        if (_listeners == null || _listeners.isEmpty())
            return;
        WizardEvent event = new WizardEvent(this);
        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            WizardListener listener = (WizardListener) it.next();
            listener.wizardCancelled(event);
        }
    }

    private void fireFinishedEvent() {
        if (_listeners == null || _listeners.isEmpty())
            return;
        WizardEvent event = new WizardEvent(this);
        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            WizardListener listener = (WizardListener) it.next();
            listener.wizardFinished(event);
        }
    }

    private void fireNextEvent() {
        if (_listeners == null || _listeners.isEmpty())
            return;
        WizardEvent event = new WizardEvent(this);
        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            WizardListener listener = (WizardListener) it.next();
            listener.wizardNext(event);
        }
    }

    private void firePreviousEvent() {
        if (_listeners == null || _listeners.isEmpty())
            return;
        WizardEvent event = new WizardEvent(this);
        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            WizardListener listener = (WizardListener) it.next();
            listener.wizardPrevious(event);
        }
    }

    private void fireStartedEvent() {
        if (_listeners == null || _listeners.isEmpty())
            return;
        WizardEvent event = new WizardEvent(this);
        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            WizardListener listener = (WizardListener) it.next();
            listener.wizardStarted(event);
        }
    }

    /**
     * @return the String that will appear on the Cancel button
     */
    protected abstract String getCancelString();

    /**
     * @return the String that will appear on the Finish button
     */
    protected abstract String getFinishString();

    /**
     * @return the wizard header panel
     */
    public AbstractWizardHeader getHeader() {
        return _header;
    }

    /**
     * @return the String that will appear on the Next button
     */
    protected abstract String getNextString();

    /**
     * @return the String that will appear on the Previous button
     */
    protected abstract String getPreviousString();

    /**
     * usually only called from the WizardValidator, after validation succeeds
     * 
     * validation always occurs when the 'next' or 'finish' button was clicked, so when the validation succeeds, the
     * wizard can go to the next page
     */
    public final void gotoNextPage() {
        next();
    }

    private void initActions() {
        _nextAction = new AbstractAction(getNextString()) {
            public void actionPerformed(ActionEvent e) {
                tryNext();
            }
        };
        _previousAction = new AbstractAction(getPreviousString()) {
            public void actionPerformed(ActionEvent e) {
                previous();
            }
        };
        _cancelAction = new AbstractAction(getCancelString()) {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        };
    }

    private void initComponents() {
        _pages = new ArrayList();
        getContentPane().setLayout(new BorderLayout(0, 0));
        _header = new WizardHeader();
        getContentPane().add(_header, BorderLayout.NORTH);
        _content = new JPanel();
        _cards = new CardLayout();
        _content.setLayout(_cards);

        getContentPane().add(_content, BorderLayout.CENTER); // was: WEST

        _buttonPanel = new JPanel();
        _buttonSeparator = new JSeparator();
        _cancelButton = new JButton();
        _previousButton = new JButton();
        _nextButton = new JButton();
        _cancelButton.setAction(_cancelAction);
        _previousButton.setAction(_previousAction);
        _nextButton.setAction(_nextAction);
        GridBagConstraints gridBagConstraints;
        _buttonPanel.setLayout(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 1.0;
        _buttonPanel.add(_buttonSeparator, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        _buttonPanel.add(_cancelButton, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        _buttonPanel.add(_previousButton, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        _buttonPanel.add(_nextButton, gridBagConstraints);
        getContentPane().add(_buttonPanel, BorderLayout.SOUTH);
    }

    private void initWindow() {
        _glassPane = new WizardGlassPane();
        setGlassPane(_glassPane);
    }

    /**
     * @return whether the wizard header is visible
     */
    public final boolean isHeaderVisible() {
        return _headerVisible;
    }

    /**
     * lock the wizard dialog, preventing any user input
     */
    public final void lock() {
        _glassPane.setVisible(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private void next() {
        if (_activePage == null)
            return;
        _activePage.passivate();
        int activeIndex = _pages.indexOf(_activePage);
        int nextIndex = activeIndex + 1;
        if (nextIndex >= _pages.size()) {
            tryFinish();
            return;
        } else {
            _activePage = (AbstractWizardPage) _pages.get(nextIndex);
            showActivePage();
        }
        fireNextEvent();
    }

    private void previous() {
        if (_activePage == null)
            return;
        _activePage.passivate();
        int activeIndex = _pages.indexOf(_activePage);
        int previousIndex = activeIndex - 1;
        if (previousIndex < 0)
            return;
        else {
            _activePage = (AbstractWizardPage) _pages.get(previousIndex);
            showActivePage();
        }
        firePreviousEvent();
    }

    public final void removeWizardListener(WizardListener listener) {
        if (listener == null || _listeners == null || !_listeners.contains(listener))
            return;
        _listeners.remove(listener);
    }

    /**
     * @param header the wizard header panel
     */
    public void setHeader(AbstractWizardHeader header) {
        if (this._header != null) {
            getContentPane().remove(header);
        }
        this._header = header;
        if (this._header != null) {
            getContentPane().add(header, BorderLayout.NORTH);
        }
    }

    /**
     * @param visible show the header in this wizard?
     */
    public final void setHeaderVisible(boolean visible) {
        _headerVisible = visible;
        if (_header != null)
            _header.setVisible(visible);
    }

    @Override
    public void setVisible(boolean visible) {
		if (visible) {
			fireStartedEvent();
			if (_pages.size() > 0) {
				_activePage = (AbstractWizardPage) _pages.get(0);
				showActivePage();
			}
		}
		super.setVisible(visible);
	}

    private void showActivePage() {
        if (_activePage == null)
            return;
        int number = _pages.indexOf(_activePage);
        // show the active page
        _cards.show(_content, "page" + number);
        // update the wizard header
        if (_header != null) {
            _header.setTitle(_activePage.getTitle());
            _header.setDescription(_activePage.getDescription());
            _header.setIcon(_activePage.getIcon());
        }
        // set visibility and localized text of buttons
        if (number == 0) {
            _previousButton.setVisible(false);
        } else {
            _previousButton.setVisible(_activePage.isPreviousVisible());
        }
        _previousAction.putValue(Action.NAME, getPreviousString());
        _cancelButton.setVisible(_activePage.isCancelVisible());
        _cancelAction.putValue(Action.NAME, getCancelString());
        _nextButton.setVisible(_activePage.isNextVisible());
        _nextAction.putValue(Action.NAME, getNextString());
        if (number + 1 == _pages.size()) {
            _nextAction.putValue(Action.NAME, getFinishString());
        } else {
            _nextAction.putValue(Action.NAME, getNextString());
        }
        if (_nextButton.isVisible()) {
            getRootPane().setDefaultButton(_nextButton);
            // workaround wrong default button (e.g. on OverviewPage)
            if (_activePage.getFocusField() == null) {
                _nextButton.grabFocus();
            }
        }
        _activePage.doActivate();
    }

    private void tryFinish() {
        if (finish()) {
            this.setVisible(false);
            fireFinishedEvent();
        }
    }

    private void tryNext() {
        if (_activePage == null)
            return;
        _activePage.validateInput();
    }

    /**
     * unlock the wizard dialog, allowing user input
     */
    public final void unlock() {
        _glassPane.setVisible(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

}