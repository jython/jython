template = """\
    public static double %(name)s(double v) throws PyException {\r
        return check(Math.%(name)s(v));\r
    }\r
\r
"""

names = ['acos', 'asin', 'atan', 'atan2', 'ceil', 'cos', 
      	'exp', 'floor', 'log', 'pow', 'sin', 'sqrt', 'tan']
		
for name in names:
	print template % {'name':name},
	
