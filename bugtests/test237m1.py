from jarray import array

from javax import swing
from java import awt
from java.beans import PropertyChangeListener

import java.lang
import string


class DlgClick(PropertyChangeListener) :
        #
        # this delegates propertyChange events
        # subclassing and interface extending doesn't seem to work
        # properly
        #
        def __init__(self, delegate) :
                self.cb = delegate

        def propertyChange(self, e) :
                self.cb(e)

class UiDialog(swing.JDialog) :

        def __init__(self, widget, title="") :
                swing.JDialog.__init__(self, swing.JOptionPane.getFrameForComponent(widget), 1)
                
                self.setTitle(title)
                self.dlg_body = None 
                self.create_body()
                
                self.dlg_option_pane = swing.JOptionPane( \
                        array(self.dlg_body, java.lang.Object), \
                        self.get_type(), \
                        self.get_options(), \
                        self.get_icon(), \
                        self.get_buttons(), \
                        self.get_default())
                        
                self.setContentPane(self.dlg_option_pane)
                self.setDefaultCloseOperation(swing.JDialog.DO_NOTHING_ON_CLOSE)
                
                self.dlg_option_pane.addPropertyChangeListener(DlgClick(self.dlg_click))
                

                self.pack()
                self.setResizable(0)
                self.show()
                
        def create_body(self) :
                if self.dlg_body is None :
                        self.dlg_body = ["This is a dialog"]
                
        def get_type(self) :
                return swing.JOptionPane.PLAIN_MESSAGE
                
        def get_options(self) :
                return swing.JOptionPane.OK_CANCEL_OPTION
                
        def get_icon(self) :
                return None
                
        def get_buttons(self) :
                return ["Ok", "Cancel"]
                
        def get_default(self) :
                return self.get_buttons()[0]
                
        def dlg_click(self, event) :

                prop = event.getPropertyName()
                value = self.dlg_option_pane.getValue()
                
                # don't run after initialisation
                if self.isVisible() and (event.getSource() == self.dlg_option_pane) \
                        and (prop == swing.JOptionPane.VALUE_PROPERTY or prop == swing.JOptionPane.INPUT_VALUE_PROPERTY) :
                        
                        # reset for new input
                        if value == swing.JOptionPane.UNINITIALIZED_VALUE :
                                return

                        self.dlg_option_pane.setValue(swing.JOptionPane.UNINITIALIZED_VALUE)
                        
                        if self.dlg_validate(value) :
                                self.dlg_make_result(value)
                                self.dispose()
                                
        def dlg_validate(self, pressed) :
                # return whether input is valid depending on pressed button
                return 1
                
        def dlg_make_result(self, pressed) :
                # the result is read afterwards to figure out the outcome of the user input
                if pressed == "Ok" :
                        self.result = 1
                else :
                        self.result = 0
