Проєкт! Для **генерування звітів**!

---

### Brainstorm

**Приклад MD:**
```markdown
---
lab.name: улкцлцукцу
lab.porpose: АААААААА
lab.conclusion: ФФФФФФФФФФ
lab.code.paths:
    - */**/**.java
student.name: Крамар А.О. 
student.group: ФЕІ-36с
teacher.name: Awqweqweweq
photos:
    file:
        imagegen:
             name: ImageGenerator.java
             label: Таке вот воно
             slice: 13..32
    maven:
        asbss:
            phase: clean test
            label: ewqwewqeeqwweweqqweqweqew
    shell:
        abbzz:
            command: python gay.py
            input: 1\n2\n3\n   
            label: qweqwewqwewqwqe
---
#### Робота про шось
кокуцкцкуцк
цукцууц
кцук
цукуццук
{{imagen}}
weqrewrew
werew
{{asbss}}
```

- вміст полів в md:
  - назва лабки
  - ім'я студента
  - група студента
  - ім'я викладача
  - назва кафедри
  - назва дисципліни
- Ідея така: ключові елементи звіту формуються з полів MD. Весь його вміст, що йде далі,
репрезентує унікальний вміст кожного звіту.

---

### Функціональні вимоги

- FR-1: Користувач має мати можливість задати структуру лабки через деякий Markdown файл і, при виклику,
сформувати деякий Docs документ + PDF файл, як готовий звіт.