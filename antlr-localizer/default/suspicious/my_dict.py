class My_dictionary(dict) :
    # __init__ function
    def __init__(self) :
        self = dict();

    # add key:value 
    def add(self, key, value) :
        self[key] = value
