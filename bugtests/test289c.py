"""

"""

import support


import test288j, test288i


class t:
	def __init__(self, s):
		self.s = s

	def __getattr__(self, name):
		return getattr(self.s, name)

class u(test288i):
	def __init__(self, s):
		self.s = s

	def get(self, i=None):
		if i:
			return self.s.get(i)
		else:
			return self.s.get()

class v(test288i):
	def __init__(self, s):
		self.s = s

	def __getattr__(self, name):
		return getattr(self.s, name)

def main():
	y = v(test288j())
	y.get()
	y.get(2)
	y.get()
	y.get(0)

if __name__ == '__main__':
	main()

#raise support.TestError("" + `x`)
