# DB Tool

Utilitário de background para Windows que acelera a escrita de SQL em editores como o
DBeaver: via atalhos globais (Alt+tecla), completa JOINs a partir das foreign keys do
Oracle, gera GROUP BY a partir do SELECT e formata a query no estilo da casa — tudo sem
precisar sair do editor.

*Background utility for Windows that speeds up writing SQL in editors like DBeaver: via
global hotkeys (Alt+key), it completes JOINs from Oracle's foreign keys, generates GROUP
BY from the SELECT list, and formats the query in house style — all without leaving the
editor.*

---

## 🇧🇷 Português

### Visão geral

O DB Tool roda em segundo plano, sem janela própria (fica só no ícone da bandeja do
Windows) e escuta atalhos globais do tipo **Alt+tecla**, que funcionam mesmo com o foco
em outro programa (ex.: DBeaver). Cada atalho lê o texto do editor via
copiar/colar/clipboard, gera o trecho de SQL correspondente e cola direto na posição do
cursor.

### Atalhos

| Atalho  | Ação                                                                                           |
|---------|------------------------------------------------------------------------------------------------|
| `Alt+C` | Completa um JOIN: digite o alias de uma tabela já declarada seguido de `.` logo após o `ON` e o atalho insere as condições de igualdade, usando as foreign keys do Oracle (ou o arquivo de relacionamentos manuais). |
| `Alt+G` | Gera o `GROUP BY` a partir das colunas do `SELECT` do statement atual.                          |
| `Alt+F` | Formata o statement onde o cursor está (SELECT/FROM/JOIN/WHERE/GROUP BY/ORDER BY no estilo da casa), qualificando as tabelas do `FROM`/`JOIN` com o *owner* configurado. |
| `Alt+V` | Varre o editor inteiro, encontra `JOIN ... ON` já escritos à mão e grava no arquivo de relacionamentos manuais os que ainda não existem lá. |

Depois de cada `Alt+C`/`Alt+G`, o statement inteiro é reformatado automaticamente (o
mesmo que o `Alt+F` faz). O ícone da bandeja mostra um balão de sucesso ou erro a cada
acionamento.

### Requisitos

- Windows (usa `SystemTray` e automação de teclado/clipboard do AWT).
- JDK 21 (para rodar/compilar; o executável empacotado já leva o runtime).
- Acesso a um banco Oracle (driver `ojdbc11`).
- Um editor de texto/SQL com foco (ex.: DBeaver) suportando os atalhos padrão de
  selecionar/copiar/colar.

### Configuração

Na primeira execução (ou pelo menu "Configurar conexão..." no ícone da bandeja) abre uma
janela para preencher:

- **Host**, **Porta**, **Serviço/SID**, **Usuário**, **Senha** — conexão Oracle.
- **Owner (tabelas)** — schema usado para qualificar as tabelas do `FROM`/`JOIN` ao
  formatar (`Alt+F`), ex. `SCHEMA.TABELA`. É opcional: deixe em branco para não
  qualificar nenhuma tabela. Não existe valor padrão fixo no código — o valor vem sempre
  do que for configurado aqui.

Esses dados são salvos em `config/db.properties` (arquivo local, fora do versionamento —
veja `config/db.properties.example` para o modelo). Alternativamente, podem vir de
variáveis de ambiente, que têm prioridade sobre o arquivo:

```
DB_HOST, DB_PORT, DB_SERVICE, DB_USERNAME, DB_PASSWORD, DB_OWNER
```

**Relacionamentos manuais:** tabelas sem FK real no Oracle podem ter o relacionamento
declarado à mão em `config/manual-relationships.conf` (um por linha):

```
TABELA_A -> TABELA_B: COL_A1=COL_B1, COL_A2=COL_B2
```

O atalho `Alt+V` ajuda a popular esse arquivo automaticamente a partir de SQL já escrito.

### Clonando e rodando

```bash
git clone <url-do-repositorio>
cd dbtool
mvn exec:java
```

Na primeira execução a janela de configuração abre sozinha. Rode `mvn test` para
executar a suíte de testes.

### Gerando o executável (Windows)

```powershell
.\build-exe.ps1
```

Requer o JDK 21 (que traz o `jpackage`) no `PATH`. O script compila o projeto com Maven e
empacota um app nativo em `dist\DBTool\DBTool.exe` — crie um atalho para ele (inclusive
na pasta Inicializar do Windows, se quiser que abra junto com o sistema).

---

## 🇬🇧 English

### Overview

DB Tool runs entirely in the background, with no window of its own (just a system tray
icon), and listens for global **Alt+key** hotkeys that work even while another
application has focus (e.g. DBeaver). Each hotkey reads the editor's text via
copy/paste/clipboard, builds the matching SQL snippet, and pastes it right at the
cursor.

### Hotkeys

| Hotkey  | Action                                                                                         |
|---------|------------------------------------------------------------------------------------------------|
| `Alt+C` | Completes a JOIN: type an already-declared table's alias followed by `.` right after `ON`, and the hotkey inserts the equality conditions, using Oracle's foreign keys (or the manual relationships file). |
| `Alt+G` | Generates the `GROUP BY` from the current statement's `SELECT` column list.                    |
| `Alt+F` | Formats the statement the cursor is in (SELECT/FROM/JOIN/WHERE/GROUP BY/ORDER BY in house style), qualifying `FROM`/`JOIN` tables with the configured *owner*. |
| `Alt+V` | Scans the whole editor for already hand-written `JOIN ... ON` clauses and saves any relationship not yet present to the manual relationships file. |

After every `Alt+C`/`Alt+G`, the whole statement is automatically reformatted (the same
thing `Alt+F` does). The tray icon shows a success or error balloon on every trigger.

### Requirements

- Windows (uses AWT's `SystemTray` and keyboard/clipboard automation).
- JDK 21 (to run/build; the packaged executable bundles its own runtime).
- Access to an Oracle database (`ojdbc11` driver).
- A focused text/SQL editor (e.g. DBeaver) supporting standard
  select/copy/paste shortcuts.

### Configuration

On first launch (or via "Configurar conexão..." in the tray menu) a window opens to fill
in:

- **Host**, **Port**, **Service/SID**, **Username**, **Password** — Oracle connection.
- **Owner (tables)** — the schema used to qualify `FROM`/`JOIN` tables when formatting
  (`Alt+F`), e.g. `SCHEMA.TABLE`. It's optional: leave it blank to skip qualifying any
  table. There is no hardcoded default in the code — the value always comes from
  whatever is configured here.

This is saved to `config/db.properties` (a local, untracked file — see
`config/db.properties.example` for the template). It can also come from environment
variables, which take priority over the file:

```
DB_HOST, DB_PORT, DB_SERVICE, DB_USERNAME, DB_PASSWORD, DB_OWNER
```

**Manual relationships:** tables with no real FK in Oracle can have their relationship
declared by hand in `config/manual-relationships.conf` (one per line):

```
TABLE_A -> TABLE_B: COL_A1=COL_B1, COL_A2=COL_B2
```

The `Alt+V` hotkey helps populate that file automatically from SQL you've already
written.

### Cloning and running

```bash
git clone <repository-url>
cd dbtool
mvn exec:java
```

On first run, the settings window opens on its own. Run `mvn test` to run the test
suite.

### Building the executable (Windows)

```powershell
.\build-exe.ps1
```

Requires JDK 21 (which bundles `jpackage`) on your `PATH`. The script compiles the
project with Maven and packages a native app at `dist\DBTool\DBTool.exe` — create a
shortcut to it (including in the Windows Startup folder, if you want it to launch with
the system).
