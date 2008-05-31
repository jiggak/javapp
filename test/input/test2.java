//#if defined(pee)
System.out.println("pee is defined");

//#if ${pee} == "pee"
System.out.println("(if) pee='${pee}'");
//#elif ${pee} == "poo"
System.out.println("(elif) pee='${pee}'");
//#endif

//#else
System.out.println("pee is undefined");
//#endif
