To deploy this application

1. Copy the jython.jar file into the WEB-INF/lib subdirectory 
2. Set the value of python.home property in WEB-INF/web.xml so 
   that it points to your jython installation.
3. Copy this directory to the applications directory of your servlet
   container. On Apache Tomcat, this is the "webapps" subdirectory
   of the tomcat install directory.
4. Enter the URL http://localhost:8080/modjy_webapp into your
   browser.
5. You should see a table containing the WSGI environment.

Please see the installation documentation for more details.

http://modjy.xhaus.com/install.html
