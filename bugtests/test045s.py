"""
Java exception thrown for non-keyword argument following keyword
"""

def parrot(**args):
    pass

parrot(voltage=5.0, 'dead')


