---
properties:
  course_name: "Інструментарій роботи з даними"
  lab_name: "Очистка та підготовка даних"
  lab_purpose: "Сформувати навички очищення даних: робота з пропусками, дублікатами та аномальними значеннями (outliers)."
  lab_conclusion: "В ході роботи було очищено 'брудний' датасет продажів. Дублікати видалено, пропуски заповнено модою/медіаною, а аномально високі суми продажів виявлено за допомогою IQR та видалено."
  lab_number: 4
  student_name: "Іваненко І.І."
  student_group: "КН-36с"
  teacher_name: "доц. Петренко П.П."
  departament_name: "Кафедра програмної інженерії"
codes:
  - "lab4_clean.py"
photos:
  bash:
    clean_process:
      label: "Процес очищення даних"
      slice: "0..50"
      runs:
        - command: "python lab4_clean.py"
  text:
    report_file:
      name: "report_variant05.txt"
      label: "Звіт про якість даних (до і після)"
  images:
    random_image:
      file: "aboba.jpg"
      label: "ІІІ"
---
## Хід роботи

**Обраний варіант:** №5 (Продажі)
- **Дані:** sale_id, manager, amount, region, date
- **Дублікати:** перевірка за sale_id
- **Пропуски:**
  - region -> заповнити модою
  - amount -> заповнити медіаною
- **Аномалії:** amount (метод IQR), дія: **mark + drop** (видалити аномалії)

### Аналіз та очищення
Було створено файл variant05.csv з навмисно внесеними помилками (NaN, дублікати ID, аномально великі суми).

Скрипт lab4_clean.py виконує наступні кроки:
1.  Приведення типів.
2.  Імпутація пропущених значень.
3.  Дедуплікація.
4.  Розрахунок меж IQR (Interquartile Range).
5.  Фільтрація викидів.

{{clean_process}}

{{report_file}}

{{random_image}}