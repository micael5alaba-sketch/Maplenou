/// One free-form label/value pair for [AddProductScreen]'s
/// "Caractéristiques" section (ex: Marque → Nike, Matière → Coton bio).
///
/// Mutable on purpose: the form edits label/value in place as the seller
/// types, one instance per row in a growable list.
class ProductSpecificationEntry {
  String label;
  String value;

  ProductSpecificationEntry({this.label = '', this.value = ''});
}
