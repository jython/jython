
from javax.servlet.http import *

import test184p


class test184s2(HttpServlet):
    def doGet(self, request, response):
        response.setContentType("text/html")
        out = response.getOutputStream()
        out.println("<html>")
        out.println("<head> <title> JPython test184s2  servlet </title></head>")
        out.println("<body> <h2> Hello World! JPython %s </h2> </body>" % test184p.foo())
        out.println(" </html>")


print test184s2, ServletConfig
