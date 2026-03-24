[Setup]
; Базові налаштування інсталятора
AppName=SPERMATOZOID Report Generator
AppVersion=1.0.0
AppPublisher=Banew
; Куди програма буде встановлюватися (C:\Program Files\reports)
DefaultDirName={autopf}\reports
DefaultGroupName=SPERMATOZOID Report Generator
; Куди Inno Setup покладе готовий інсталятор
OutputDir=release\installer
; Як буде називатися сам інсталятор
OutputBaseFilename=reportsSetup
Compression=lzma2
SolidCompression=yes
; Нам потрібні права адміна, щоб писати в Program Files і змінювати глобальний PATH
PrivilegesRequired=admin
; Кажемо Windows миттєво оновити змінні середовища після встановлення
ChangesEnvironment=yes

[Files]
; Беремо ВСІ файли з папки reports, яку згенерував jpackage, і кладемо в папку встановлення
Source: "release\reports\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; Робимо акуратні ярлики в меню Пуск
Name: "{group}\SPERMATOZOID Report Generator"; Filename: "{app}\reports.exe"
Name: "{group}\Uninstall SPERMATOZOID Report Generator"; Filename: "{uninstallexe}"

[Code]
// --- ПОЧАТОК МАГІЇ З PATH ---
const
  EnvironmentKey = 'SYSTEM\CurrentControlSet\Control\Session Manager\Environment';

// Функція, яка акуратно вирізає наш шлях з PATH при видаленні програми
procedure RemovePath(Path: string);
var
  Paths: string;
  P: Integer;
begin
  if not RegQueryStringValue(HKEY_LOCAL_MACHINE, EnvironmentKey, 'Path', Paths) then
    Exit;

  P := Pos(';' + Path, Paths);
  if P = 0 then P := Pos(Path + ';', Paths);
  if P = 0 then P := Pos(Path, Paths);

  if P > 0 then
  begin
    Delete(Paths, P, Length(Path) + 1);
    RegWriteStringValue(HKEY_LOCAL_MACHINE, EnvironmentKey, 'Path', Paths);
  end;
end;

// Додаємо шлях у PATH відразу після копіювання файлів
procedure CurStepChanged(CurStep: TSetupStep);
var
  Paths: string;
begin
  if CurStep = ssPostInstall then
  begin
    if RegQueryStringValue(HKEY_LOCAL_MACHINE, EnvironmentKey, 'Path', Paths) then
    begin
      // Перевіряємо, чи ми вже не додавали цей шлях раніше
      if Pos(ExpandConstant('{app}'), Paths) = 0 then
      begin
        Paths := Paths + ';' + ExpandConstant('{app}');
        RegWriteStringValue(HKEY_LOCAL_MACHINE, EnvironmentKey, 'Path', Paths);
      end;
    end;
  end;
end;

// Запускаємо очистку PATH після того, як юзер натиснув "Видалити"
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
  begin
    RemovePath(ExpandConstant('{app}'));
  end;
end;