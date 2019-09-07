// Based on CLDR data:
// https://github.com/unicode-org/cldr/blob/13cd95684a52cd18c1238574ca519c335758e503/common/supplemental/ordinals.xml
//
// Covers all the languages available for Metrodroid except Maori (mi)

package au.id.micolous.metrodroid.multi

object Plurals {
  fun getQuantityString(lang: String, count: Int): String = when (lang) {
    "bm", "bo", "dz", "id", "ig", "ii", "in", "ja", "jbo", "jv", "jw", "kde", "kea", "km", "ko", "lkt", "lo", "ms", "my", "nqo", "osa", "root", "sah", "ses", "sg", "su", "th", "to", "vi", "wo", "yo", "yue", "zh" -> "other"
    "pt", "ff", "fr", "hy", "kab", "am", "as", "bn", "fa", "gu", "hi", "kn", "zu" -> when (count) {
      0, 1 -> "one"
      else -> "other"
    }
    "da", "af", "an", "asa", "az", "bem", "bez", "bg", "brx", "ce", "cgg", "chr", "ckb", "dv", "ee", "el", "eo", "es", "eu", "fo", "fur", "gsw", "ha", "haw", "hu", "jgo", "jmc", "ka", "kaj", "kcg", "kk", "kkj", "kl", "ks", "ksb", "ku", "ky", "lb", "lg", "mas", "mgo", "ml", "mn", "mr", "nah", "nb", "nd", "ne", "nn", "nnh", "no", "nr", "ny", "nyn", "om", "or", "os", "pap", "ps", "rm", "rof", "rwk", "saq", "sd", "sdh", "seh", "sn", "so", "sq", "ss", "ssy", "st", "syr", "ta", "te", "teo", "tig", "tk", "tn", "tr", "ts", "ug", "uz", "ve", "vo", "vun", "wae", "xh", "xog", "ak", "bh", "guw", "ln", "mg", "nso", "pa", "ti", "wa", "si", "ast", "ca", "de", "en", "et", "fi", "fy", "gl", "ia", "io", "it", "ji", "nl", "pt_PT", "sc", "scn", "sv", "sw", "ur", "yi" -> when (count) {
      1 -> "one"
      else -> "other"
    }
    "tzm" -> when (count) {
        in 0..1, in 11..99 -> "one"
        else -> "other"
    }
    "is", "mk" -> when {
      count % 10 == 1 && count % 100 != 11 -> "one"
      else -> "other"
    }
    "ceb", "fil", "tl" -> when {
      count % 10 !in listOf(4,6,9) -> "one"
      else -> "other"
    }
    "lv", "prg" -> when {
      count % 10 == 0 || (count % 100) in 11..19 -> "zero"
      count % 10 == 1 && (count % 100) != 11 -> "one"
      else -> "other"
    }
    "lag", "ksh" -> when (count) {
      0 -> "zero"
      1 -> "one"
      else -> "other"
    }
    "iu", "naq", "se", "sma", "smi", "smj", "smn", "sms" -> when (count) {
      1 -> "one"
      2 -> "two"
      else -> "other"
    }
    "shi" -> when (count) {
      in 0..1 -> "one"
      in 2..10 -> "few"
      else -> "other"
    }
    "mo", "ro" -> when {
      count == 1 -> "one"
      count == 0 || count % 100 in 2..19 -> "few"
      else -> "other"
    }
    "bs", "hr", "sh", "sr" -> when {
      count % 10 == 1 && count % 100 != 11 -> "one"
      count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
      else -> "other"
    }
    "gd" -> when (count) {
      1,11 -> "one"
      2,12 -> "two"
      in 3..10, in 13..19 -> "few"
      else -> "other"
    }
    "dsb", "hsb", "sl" -> when (count % 100) {
      1 -> "one"
      2 -> "two"
      in 3..4 -> "few"
      else -> "other"
    }
    "he", "iw" -> when {
      count == 1 -> "one"
      count == 2 -> "two"
      count !in 0..10 && count % 10 == 0 -> "many"
      else -> "other"
    }
    "cs", "sk" -> when (count) {
      1 -> "one"
      in 2..4 -> "few"
      else -> "other"
    }
    "pl" -> when {
      count == 1 -> "one"
      count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
      count != 1 && count % 10 in 0..1 || count % 10 in 5..9 || count % 100 in 12..14 -> "many"
      else -> "other"
    }
    "be" -> when {
      count % 10 == 1 && count % 100 != 11 -> "one"
      count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
      count % 10 == 0 || count % 10 in 5..9 || count % 100 in 11..14 -> "many"
      else -> "other"
    }
    "lt" -> when {
      count % 10 == 1 && count % 100 !in 11..19 -> "one"
      count % 10 in 2..9 && count % 100 !in 11..19 -> "few"
      else -> "other"
    }
    "mt" -> when {
      count == 1 -> "one"
      count == 0 || count % 100 in 2..10 -> "few"
      count % 100 in 11..19 -> "many"
      else -> "other"
    }
    "ru", "uk" -> when {
      count % 10 == 1 && count % 100 != 11 -> "one"
      count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
      count % 10 == 0 || count % 10 in 5..9 || count % 100 in 11..14 -> "many"
      else -> "other"
    }
    "br" -> when {
      count % 10 == 1 && count % 100 !in listOf(11,71,91) -> "one"
      count % 10 == 2 && count % 100 !in listOf(12,72,92) -> "two"
      count % 10 in listOf(3,4,9) && (count % 100 / 10) !in listOf(1,7,9) -> "few"
      count != 0 && count % 1000000 == 0 -> "many"
      else -> "other"
    }
    "ga" -> when (count) {
      1 -> "one"
      2 -> "two"
      in 3..6 -> "few"
      in 7..10 -> "many"
      else -> "other"
    }
    "gv" -> when {
      count % 10 == 1 -> "one"
      count % 10 == 2 -> "two"
      count % 100 in listOf(0,20,40,60,80) -> "few"
      else -> "other"
    }
    "ar", "ars" -> when {
      count == 0 -> "zero"
      count == 1 -> "one"
      count == 2 -> "two"
      count % 100 in 3..10 -> "few"
      count % 100 in 11..99 -> "many"
      else -> "other"
    }
    "cy" -> when (count) {
      0 -> "zero"
      1 -> "one"
      2 -> "two"
      3 -> "few"
      6 -> "many"
      else -> "other"
    }
    "kw" -> when {
      count == 0 -> "zero"
      count == 1 -> "one"
      count % 100 in listOf(2,22,42,62,82) || count % 1000 == 0 && (count % 100000 in 1000..20000 || count in listOf(40000,60000,80000)) || count!=0 && count % 1000000==100000 -> "two"
      count % 100 in listOf(3,23,43,63,83) -> "few"
      count != 1 && count % 100 in listOf(1,21,41,61,81) -> "many"
      else -> "other"
    }
    else -> "other"
  }
}
