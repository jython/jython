
from javax import swing

class test189c(swing.ListCellRenderer):
    def getListCellRendererComponent(self):
        return "test189c"


class test189c2(test189c):
    def getListCellRendererComponent(self):
        return "test189c2"


print test189c().getListCellRendererComponent()
print test189c2().getListCellRendererComponent()