
from javax.servlet.http import HttpServlet

import test184p

class test184s1(HttpServlet):
    def doGet(self, request, response):
        response.setContentType("text/html")
        out = response.getOutputStream()
        out.println("<html>")
        out.println("<head> <title> JPython test184s1 servlet </title></head>")
        out.println("<body> <h2> Hello World! JPython %s </h2> </body>" % test184p.foo())
        out.println(" </html>")


print test184s1, Exception
