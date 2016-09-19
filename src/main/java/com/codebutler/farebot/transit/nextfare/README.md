# Cubic Nextfare

This implements some base fare reader capability for Cubic Nextfare.

This is used by Go card (South East Queensland) and Transit Access Pass (Los Angeles).

This defaults to a fare system where the currency is represented in dollars. Individual systems should subclass some parts of this.  For an example, see Go card (seq_go package).