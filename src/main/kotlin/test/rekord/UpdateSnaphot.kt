package test.rekord

import java.lang.reflect.Method

@Retention(AnnotationRetention.RUNTIME)
annotation class UpdateSnaphot


fun isUpdate(method: Method): Boolean {
    val hasAnnotation = method.annotations
        .any { it.annotationClass == UpdateSnaphot::class }
    println("hasAnnotation = ${hasAnnotation}")

    val prop: String = System.getProperties().getProperty(TESTS_REKORDS_UPDATE_SNAPSHOT, "false")
    val hasProp = prop == "true"
    println("hasProp $TESTS_REKORDS_UPDATE_SNAPSHOT = ${hasProp}")

    val prop2: String = System.getProperties().getProperty(TESTS_REKORDS_SNAPSHOT, "false")
    val hasProp2 = prop2 == "update"
    println("hasProp2 $TESTS_REKORDS_SNAPSHOT = ${hasProp2}")
    return hasAnnotation || hasProp || hasProp2
}
