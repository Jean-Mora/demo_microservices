package com.pucetec.diary.exceptions

// 404: esa entrada no existe. Ni para ti, ni para nadie.
class EntryNotFoundException(message: String) : RuntimeException(message)

// 403: la entrada existe perfectamente, sé quién eres, y no es tuya.
class NotYourEntryException(message: String) : RuntimeException(message)
