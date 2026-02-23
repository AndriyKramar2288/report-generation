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
      label: "Запит"
      runs:
        - command: "python lab4_clean.py"
          input: "Yes"
---
{{clean_process}}