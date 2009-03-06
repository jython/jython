import sys

def escape_html(s): return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

def cutoff(s, n=100):
    if len(s) > n: return s[:n]+ '.. cut ..'
    return s

def handler(environ, start_response):
    writer = start_response("200 OK", [ ('content-type', 'text/html') ])
    response_parts = []
    response_parts.append("<html>")
    response_parts.append("<head>")
    response_parts.append("<title>Modjy demo application</title>")
    response_parts.append("</head>")
    response_parts.append("<body>")
    response_parts.append("<p>Modjy servlet running correctly: jython %s on %s:</p>" % (sys.version, sys.platform))
    response_parts.append("<h3>Hello WSGI World!</h3>")
    response_parts.append("<h4>Here are the contents of the WSGI environment</h4>")
    environ_str = "<table border='1'>"
    keys = environ.keys()
    keys.sort()
    for ix, name in enumerate(keys):
        if ix % 2:
            background='#ffffff'
        else:
            background='#eeeeee'
        style = " style='background-color:%s;'" % background
        value = escape_html(cutoff(str(environ[name]))) or '&#160;'
        environ_str = "%s\n<tr><td%s>%s</td><td%s>%s</td></tr>" % \
            (environ_str, style, name, style, value)
    environ_str = "%s\n</table>" % environ_str
    response_parts.append(environ_str)
    response_parts.append("</body>")
    response_parts.append("</html>")
    response_text = "\n".join(response_parts)
    return [response_text]
