import java

# Define a Python class that subclasses the Applet class
# (java.applet.Applet)

class test186a (java.applet.Applet):
     def __init__(self):
         self.list = java.awt.List()
         self.add(self.list)
     def init(self):
         self.list.add("Init called")
     def destroy(self):
         self.list.add("Destroy called")
     def start(self):
         self.list.add("Start called")
     def stop(self):
         self.list.add("Stop called")
