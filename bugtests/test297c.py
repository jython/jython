from javax.swing.text.html import HTMLEditorKit, FormView

class TestEditorKit(HTMLEditorKit):
    def getViewFactory(self):
        return self.TestFactory()

    class TestFactory(HTMLEditorKit.HTMLFactory):
        def create(self,e):
            o = e.getAttributes().getAttribute(StyleConstants.NameAttribute)
            if o == HTML.Tag.INPUT:
                return TestFormView(e)
            return HTMLEditorKit.HTMLFactory.create(self,e)
            
class TestFormView(FormView):
    def __init__(self,e):
            FormView.__init__(self,e)

    def test(self):
        print 'hello'

    def actionPerformed(self,e):
        self.test()

