import java

class TempConverter(java.lang.Object):
	def __init__(self):
		self.setFahrenheit(0.0)

	def setFahrenheit(self, degrees):
		"@sig public void setFahrenheit(double degrees)"
		self.f = degrees
		self.c = (degrees-32.)/1.8

	def getFahrenheit(self):
		"@sig public double getFahrenheit()"
		return self.f

	def setCelsius(self, degrees):
		"@sig public void setCelsius(double degrees)"
		self.c = degrees
		self.f = degrees*1.8+32.

	def getCelsius(self):
		"@sig public double getCelsius()"
		return self.c

	def __repr__(self):
		return '<%.2g degrees fahrenheit == %.2g celsius>' % (self.f, self.c)

if __name__ == '__main__':
	c = TempConverter()
	print c
	c.setCelsius(100)
	print c
	c.setCelsius(0)
	print c
	c.setFahrenheit(212)
	print c