
import support

import test182j

class test182c(test182j):
    def tstOverridePublic(self):
	#print "her1"
	return "test182c." + test182j.tstOverridePublic(self)
    def tstOverrideProtected(self):
	#print "her2"
	return "test182c." + self.super__tstOverrideProtected()
    def tstOverrideFinalProtected(self):
	return "test182c." + self.super__tstOverrideFinalProtected()
    def tstOverrideFinalPublic(self):
	return "test182c." + test182j.tstOverrideFinalPublic(self)

    def tstAbstractPublic(self):
	return "test182c.tstAbstractPublic"
    def tstAbstractProtected(self):
	return "test182c.tstAbstractProtected"

i = test182c()

support.compare(i.tstPublic(), "tstPublic")
support.compare(i.tstProtected(), "tstProtected")
support.compare(i.super__tstFinalProtected(), "tstFinalProtected")
support.compare(i.tstFinalPublic(), "tstFinalPublic")


support.compare(i.tstOverridePublic(), "test182c.tstOverridePublic")
support.compare(i.tstOverrideProtected(), "test182c.tstOverrideProtected")
support.compare(i.tstOverrideFinalProtected(), "test182c.tstOverrideFinalProtected")
support.compare(i.tstOverrideFinalPublic(), "test182c.tstOverrideFinalPublic")


support.compare(i.tstAbstractPublic(), "test182c.tstAbstractPublic")
support.compare(i.tstAbstractProtected(), "test182c.tstAbstractProtected")
