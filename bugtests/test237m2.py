import test237m1
from javax import swing
from java import awt
import string

class PSetNameDlg(test237m1.UiDialog) :

        def __init__(self, frame, label="Enter a name for the property set:", must_not_exist=1, must_exist=0) :
                self.must_exist = must_exist
                self.must_not_exist = must_not_exist
                self.label_text = label
                
                test237m1.UiDialog.__init__(self, frame, title="property set name")
                
        def create_body(self) :
                self.ps_name_input = swing.JTextField(10)
                self.label_widget = swing.JLabel(self.label_text)
                

                if self.dlg_body is None :
                        self.dlg_body = [self.label_widget, self.ps_name_input]

                
        def dlg_validate(self, pressed) :
                # check whether the name is correct
                if pressed != "Ok" :
                        return 1
                        
                name = self.ps_name_input.getText()
                
                # comment this out for testing
                #if len(name) < 1 :
                #       error = "A reasonbale name should at least have one character."
                #       test237m1.ErrorMsg(self, error)
                #       return 0        
                #if self.must_not_exist :
                #       if conf.main.psb.find_property_set(name) is not None :
                #               error = "There is already a property set with this name."
                #               test237m1.ErrorMsg(self, error)
                #               return 0
                #if self.must_exist :
                #       if conf.main.psb.find_property_set(name) is None :
                #               error = "There is no property set with this name."
                #               test237m1.ErrorMsg(self, error)
                #               return 0
                # comment out until here
                

                return 1
                
        def dlg_make_result(self, pressed) :
                # input value as result or None for cancel
                if pressed != "Ok" :
                        self.result = None
                else :
                        self.result = self.ps_name_input.getText()