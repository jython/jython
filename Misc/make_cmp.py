"""generates code for compare nodes for CodeCompiler.java"""

code = """	static Integer %(name)s;
	public Object %(name)s_cmp(SimpleNode node) throws Exception {
		if (mrefs.%(name)s == null) %(name)s = new Integer(make_binop("%(name)s"));
		return mrefs.%(name)s;
	}
"""

for name in ['less', 'greater', 'equal', 'less_equal', 
			'greater_equal', 'notequal', 'in', 'not_in',
			'is', 'is_not']:
	print code % {'name':name}

