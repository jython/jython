"""

"""

import support


import java
from pawt import swing, test

class TableModel0(swing.table.AbstractTableModel):
        columnNames = "First Name", "Last Name","Sport","# of Years","Vegetarian"
        data = [("Mary", "Campione", "Snowboarding", 5, java.lang.Boolean(0))]

        def getColumnCount(self):
                return len(self.columnNames)
                   
        def getRowCount(self):
                return len(self.data)
                
        def getColumnName(self, col):
                return self.columnNames[col]

        def getValueAt(self, row, col):
                return self.data[row][col]
                
        def getColumnClass(self, c):
                return java.lang.Class.getClass(self.getValueAt(0, c))
                
        def isCellEditable(self, row, col):
                return col >= 2
                                   
model0 = TableModel0()
support.compare(model0.getColumnClass(0), "java.lang.String")
support.compare(model0.getColumnClass(1), "java.lang.String")
support.compare(model0.getColumnClass(2), "java.lang.String")
support.compare(model0.getColumnClass(3), "java.lang.Integer")
support.compare(model0.getColumnClass(4), "java.lang.Boolean")
