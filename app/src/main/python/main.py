def sub():
    raise ValueError('you lose')

def test(msg):
    sub()
    return f'received "{msg}"'
