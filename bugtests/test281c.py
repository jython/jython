"""

"""

import support


if __name__ == "__main__": 
  try: 
     raise "me" 
  except:
     pass
  else: 
     raise support.TestError("Should not happen")


