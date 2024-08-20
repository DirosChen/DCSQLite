# DCSQLite

一个简单的android下的sqlite库，100%用kotlin实现。

## 依赖
```
implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.4")
implementation("com.fasterxml.jackson.core:jackson-core:2.13.4")
implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
implementation("com.google.guava:guava:33.2.1-jre")
implementation(kotlin("reflect"))
```

## 使用
***
### 初始化

```
   DCSQLite.Builder().context(context)
        .path(db)
        .version(1)
        .cache(true)
        .table("student(id integer PRIMARY KEY AUTOINCREMENT,name varchar,age integer,sex varchar)",
            "book(id long PRIMARY KEY,bookname varchar,price double)")
        .alter("student" addColumn "weight" type "varchar", "student" addColumn "height" type "integer")
        .build()
```

db是唯一的会话id即数据库的实际路径的字符串，如：
```
private val db = "${Environment.getExternalStorageDirectory().absolutePath}/test.db"
```

### 打开和关闭

```
db.openSQL()
db.closeSQL()
```

### 概要说明

查询数据会用到bean对象，由于使用了反射实现，所以必须提供一个默认的构造函数

```
@Table
data class Student(
    @Primary(true)
    @Column("id")
    var id: Long? = null,
    @Column("name")
    var name: String,
    @Column("sex")
    var sex: String,
    @Column("age")
    var age: Long,
    var score: Long = 0) {
        constructor() : this(null, "", "", 0)
}
```
* bean顶端需要用@Table标注为一个表映射。 @Column("id")注解代表id是对应数据库中的列名，@Primary(true)代表该列是否为主键，并是否可以自动生成。目前暂未实现外键的id自增。

* 返回结果有两种可选，第一是Flow，第二是普通的。所有结果集均是List<T>。如果查询，那么T应为数据bean，map集合，List<Any?>类型；如果为增删改和事务，那么T为Long（返回的主键ID或影响的行数）;如果为原生，那么T应为Unit，即不返回任何结果。底层使用execSQL处理。

* 每次调用方法需要先选择表，useDB或useDBFlow。调用方法都是suspend，所以需要在协程中执行。

* 支持查询缓存机制，缓存查询语句最多50条，当有增删改和事务时会清空缓存(即使内部报错)

### 查询
```
db.useDB<Student> { ("id" to 1) selectTo "select * from $T_STUDENT where id=#{id}" } //select * from t_student where id=1
db.useDB<Student> { mapOf("id" to 1, "name" to null) selectTo "select * from $T_BOOK where id=#{id} and bookname=#{name}" } // select * from t_book where id=1 and bookname=null
db.useDBFlow<Student> {T_STUDENT.select()}.catch { listOf<Student>() } //select * from select
```

### 插入
```
db.useDBFlow<Long> { Student(name = "Bom", age = 30, sex = "man") insertTo T_STUDENT}
db.useDB<Long> { listOf(Student(name = "Lily", age = 22, sex = "woman"), Student(name = "Eddie", age = 40, sex = "man")) insertTo T_STUDENT}
db.useDBFlow<Long> { mapOf("id" to 33, "name" to "Edison", "age" to 101, "sex" to "man") insertTo T_STUDENT conflict SQLiteDatabase.CONFLICT_REPLACE}
```
### 更新
```
db.useDB<Long> { "price" to 67.00 updateTo T_BOOK whereAnd ("id" to 2)}
db.useDB<Long> { "price" to 70.00 updateTo T_BOOK whereAnd setOf("id" toNot 1, "id" toNot 3) }
db.useDB<Long> { mapOf("age" to 110, "name" to "Super Edison") updateTo T_STUDENT whereAnd ("id" to 33) whereOr ("age" to  101)}
```
whereAnd和whereOr后面可继续多次使用or或者and，"price" to 67.00 updateTo T_BOOK whereAnd ("id" to 2)} or mapOf(xxx) and (x to x)，条件可使用map或set集合

### 删除
```
db.useDBFlow<Long> { T_STUDENT deleteWhereAnd ("id" to 1) and ("name" to "Lily") }
db.useDBFlow<Long> { T_STUDENT deleteWhereOr ("id" to 2) or ("id" to 33) }
db.useDBFlow<Long> { T_BOOK.delete() }
```
删除语句和更新语句很相似，deleteWhereAnd和deleteWhereOr后面可继续多次使用or或者and，条件中可使用map或set集合

### 原生
```
db.useDB<Unit> { SQL.runNativeSQL(NATIVE_READ)}
```
### 事务
```
db.useDB<Long> {
transaction( Student(name = "Super Man", age = 36, sex = "man") insertTo T_STUDENT,
Book(666, "The C Program", 55.55) insertTo T_BOOK,
"name" to "Super Girl" updateTo T_STUDENT whereAnd ("id" to 1))
}

db.useDB<Long> {
listOf("aaa", "bbb").forTransaction {i, t ->
"name" to t updateTo T_STUDENT whereAnd ("id" to "${i+1}")
}
}
```
