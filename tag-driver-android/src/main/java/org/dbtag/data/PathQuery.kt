package org.dbtag.data

object PathQuery {
//    fun parse(s: String): Filter {
//        val requiredTags: Array<String>?
//        val excludedTags: Array<String>?
//        var f = s.indexOf('?')
//        if (f == -1) {
//            requiredTags = if (s.isEmpty()) null else arrayOf(s)
//            excludedTags = null
//        }
//        else {
//            // There will follow required (+) and excluded(-) tags
//            val rt = mutableListOf<String>()
//            rt.add(s.substring(0, f))
//            f += 1
//            var ut: MutableList<String>? = null
//            val tLen = s.length
//            while (f < tLen) {
//                var ch = s[f]
//                if (ch == '+' || ch == '-') {
//                    if (++f == tLen)
//                        break
//                }
//                else
//                    ch = '+'
//                var f2 = f
//                while (f2 < tLen) {
//                    val ch2 = s[f2]
//                    if (ch2 == '+' || ch2 == '-')
//                        break
//                    ++f2
//                }
//                if (f2 != f) {
//                    val t = s.substring(f, f2)
//                    if (ch == '+')
//                        rt.add(t)
//                    else {
//                        if (ut == null)
//                            ut = mutableListOf<String>()
//                        ut.add(t)
//                    }
//                }
//                f = f2
//            }
//            requiredTags = rt.toTypedArray()
//            excludedTags = if (ut == null) null else ut.toTypedArray()
//        }
//        return Filter().require(*requiredTags!!).exclude(*excludedTags!!)
//    }
}
