# Baku Electronics QA Automation

Bu layihə `www.bakuelectronics.az` üçün Java, Selenium, TestNG, REST Assured və Maven əsaslı UI, API və API–UI integration avtomatlaşdırmasıdır. Bu sənəd yalnız repository-dəki real fayllara, `pom.xml`-ə, TestNG XML-lərinə və mənbə koduna əsaslanır. README-də göstərilən hər əmr icra edilmiş hesab olunmur; faktiki icra olunan əmrlər [audit qeydi](#17-audit-və-faktiki-yoxlamalar) bölməsində ayrıca göstərilir.

## 1. Layihə haqqında

Layihənin əsas qatları:

- Selenium Page Object Model ilə UI yoxlamaları;
- REST Assured ilə public və authenticated API yoxlamaları;
- API-dən gələn məhsul məlumatının UI ilə müqayisəsi;
- API səbətinin UI hesab səbəti ilə inteqrasiya yoxlaması;
- TestNG suite, group, retry və Allure nəticələri.

Əsas risk: login və səbət ssenariləri canlı hostlara və real hesab vəziyyətinə toxuna bilər. Xüsusilə səbətə məhsul əlavə edən, sayını dəyişən və silən testlər tam təhlükəsiz və dry-run kimi qəbul edilməməlidir.

## 2. Texnologiyalar və versiyalar

Maven compiler source/target `23` olaraq `pom.xml`-də təyin edilib. Bu, işlək mühitdə JDK 23-ün (həm `java`, həm də `javac`) uyğun seçim olduğunu göstərir; konkret kompüterdə quraşdırılmış versiya ayrıca yoxlanmalıdır. Java və Maven versiyaları zamanla dəyişə bilər.

| Komponent | `pom.xml`-dəki dəyər |
|---|---|
| Java compiler source/target | `23` |
| Selenium Java | `4.51.0` |
| TestNG | `7.10.2` |
| WebDriverManager | `5.9.3` |
| REST Assured | `5.5.0` |
| Allure BOM / TestNG | `3.0.0` |
| Allure Maven plugin | `3.0.2` |
| Maven Surefire plugin | `3.5.2` |
| SLF4J Simple | `1.7.36`, test scope |

Maven wrapper (`mvnw.cmd`) repository-də yoxdur. Buna görə `mvn` sistem PATH-ında olmalı və ya IntelliJ IDEA-nın Maven executable yolu seçilməlidir. Chrome layihənin UI testləri üçün lazımdır; WebDriverManager ChromeDriver hazırlığını idarə edir, lakin browser/driver və şəbəkə problemi zamanı əvvəlcə faktiki xəta araşdırılmalıdır.

## 3. Layihənin qovluq strukturu

```text
Bakuelectronics.az/
├── .allure/                         # repository-də saxlanan lokal Allure CLI artefaktı
├── .gitignore
├── pom.xml
├── README.md
├── scripts/
│   └── Test-ConfiguredCredentials.ps1
├── testng.xml
├── testng-parallel-ui-pilot.xml
├── testng-safe-full-run-with-integration.xml
├── src/main/java/az/bakuelectronics/automation/
│   ├── api/       ApiAuthProvider, ApiClient, ApiConfig, ProductApiService
│   ├── config/    ConfigReader
│   ├── driver/    DriverManager
│   ├── models/    ProductApiModel
│   ├── pages/     CartPage, CategoryPage, HomePage, LoginPage, ProductPage, SearchPage
│   └── utils/     ScreenshotUtils, WaitUtils
├── src/test/java/az/bakuelectronics/automation/
│   ├── api/         API testləri və basket cleanup dəstəyi
│   ├── integration/ API–UI integration testləri
│   ├── support/     BaseTest, retry, Allure listener, JSON və utility testləri
│   └── ui/          Selenium UI testləri
├── src/test/resources/
│   ├── allure.properties
│   └── testdata/negative-search-data.json
└── target/                          # Maven tərəfindən generasiya olunan artefaktlar
```

`.gitignore` `.env`, `.env.*` (`.env.example` istisna olmaqla), `*.secret`, `*.secrets` və `config/local.properties` fayllarını nəzərdə tutur. Repository-də `.env.example` yoxdur. Java kodu bu faylları avtomatik oxumur; credential-lər aşağıdakı environment variable-lardan alınır.

## 4. İlk quraşdırma və başladılma

### 4.1. Layihə qovluğuna keçid

Adi PowerShell-də:

```powershell
Set-Location -LiteralPath 'C:\Users\OMAR\Bakuelectronics.az'
Get-Location
Get-ChildItem -Force
```

Uğurlu nəticə cari qovluğun repository kökü olması və `pom.xml`, `src`, `testng.xml` fayllarının görünməsidir. Fərqli kompüterdə yolu öz checkout yolunuzla əvəz edin. `Get-ChildItem` lokal fayl sistemini dəyişmir.

Konkret faylları yoxlamaq üçün:

```powershell
@('pom.xml', 'README.md', 'testng.xml', 'scripts\Test-ConfiguredCredentials.ps1') |
    ForEach-Object {
        '{0}: {1}' -f $_, (Test-Path -LiteralPath $_ -PathType Leaf)
    }
```

Fayl strukturuna baxmaq üçün:

```powershell
rg --files -g '!target' -g '!node_modules' | Sort-Object
```

### 4.2. Java, `JAVA_HOME`, PATH və Maven diaqnostikası

```powershell
java -version
javac -version
$env:JAVA_HOME
Get-Command java, javac, mvn -ErrorAction SilentlyContinue |
    Select-Object Name, Source, CommandType
($env:Path -split ';' | Where-Object { $_ -match 'Java|jdk|maven|Maven' })
mvn -version
```

`java -version` və `javac -version` JDK-nın işlək olduğunu göstərir. `JAVA_HOME` JDK kökünə işarə etməlidir; `bin` qovluğuna yox. `mvn -version` ayrıca Maven-in hansı Java executable/version ilə işlədiyini göstərir və `java -version` nəticəsindən fərqli ola bilər. `mvn` tapılmırsa Maven PATH-ə əlavə edilməli və ya IntelliJ-də Maven home seçilməlidir.

PowerShell-də dəyişən adını yoxlamaq üçün `$env:JAVA_HOME` dəyəri görünür; bu dəyər secret deyil, amma səhv və ya boş ola bilər. PATH bütün dəyərləri çap etdiyinə görə bunu paylaşılmış loga kor-koranə köçürməyin.

Maven wrapper-ın olub-olmadığını təsdiqləyin:

```powershell
Test-Path -LiteralPath '.\mvnw.cmd' -PathType Leaf
Test-Path -LiteralPath '.\mvnw' -PathType Leaf
```

Bu layihədə hər iki yoxlama `False` qaytarır; buna görə `mvnw.cmd` komandaları sənədləşdirilmir.

### 4.3. IntelliJ IDEA terminalı ilə adi PowerShell fərqi

Windows User Environment Variables dəyişdirildikdən sonra artıq açıq PowerShell və IntelliJ IDEA prosesləri köhnə environment snapshot-ını saxlaya bilər. Dəyişəndən sonra yeni PowerShell açın; IntelliJ üçün IDE-ni tam bağlayıb yenidən açın. Yalnız Run Configuration-a verilmiş dəyişənlər həmin run üçün keçərli ola bilər və sistem PowerShell-ə avtomatik yayılmaya bilər.

IntelliJ-də aşağıdakıları yoxlayın:

1. `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven` bölməsində Maven home və JDK.
2. `Settings`-də Maven Runner üçün JDK-nın compiler tələbi ilə uyğunluğu.
3. Test Run Configuration → `Environment variables` sahəsində yalnız lokal credential adlarının və dəyərlərinin verilməsi.

Parolu `pom.xml`, README, source code, terminal command line və shell history-yə yazmayın.

### 4.4. Dependency və layihə modelinin yoxlanması

```powershell
Get-Content -Raw -LiteralPath '.\pom.xml'
mvn help:effective-pom -Doutput=target/effective-pom.xml
mvn dependency:tree
mvn -q -DskipTests test-compile
```

`effective-pom.xml` və dependency tree `target` altında yaradılan/yenilənən lokal artefaktlardır. Əgər yalnız mövcud fayllara toxunmadan baxmaq istəyirsinizsə, ilk `Get-Content` və `mvn dependency:tree` kifayətdir; `help:effective-pom` output faylı yaradır.

`test-compile` həm main, həm test source kodunu compile edir, test metodlarını işə salmır. Uğurlu nəticə Maven-in `BUILD SUCCESS` yazmasıdır. `Could not resolve dependency` xətasında repository, proxy, DNS, TLS və Maven local cache yoxlanır; dependency versiyasını dəyişmək ilkin həll deyil.

## 5. Java və Maven konfiqurasiyası

`pom.xml` Java 23 source/target tələb edir. Bu, mütləq olaraq bütün kompüterlərdə eyni patch versiyası olmalıdır demək deyil; compiler, Maven JVM-i və IDE JDK-sı uyğun olmalıdır.

Maven-in istifadə etdiyi Java-nı yoxlayın:

```powershell
mvn -version
```

Əgər burada Java 23-dən fərqli JVM görünürsə:

1. `JAVA_HOME`-un JDK kökünə işarə etdiyini yoxlayın.
2. PATH-da köhnə Java shim/entry-nin əvvəl gəlmədiyini yoxlayın.
3. IntelliJ Maven Runner JDK-sını yoxlayın.
4. Dəyişiklikdən sonra yeni terminal/IDE prosesi açın.
5. Yenidən `java -version`, `javac -version`, `mvn -version` və `mvn -q -DskipTests test-compile` çalışdırın.

## 6. Credential və environment variable-lar

### Credential və Environment Variable Problemlərinin Əvvəldən Axıra Qədər Həlli

Authenticated API və tam login UI axını bu iki dəyişəni tələb edir:

```text
BAKU_ELECTRONICS_LOGIN
BAKU_ELECTRONICS_PASSWORD
```

README-yə və command line-a real dəyər yazılmır.

### 6.1. Credential yoxlaması niyə test başlamadan görünə bilər?

TestNG əvvəlcə test siniflərini kəşf edir və instansiyalaşdırır. Əgər constructor və ya field initializer daxilində authentication sorğusu göndərilsəydi, credential/network xətası `@Test` metodu başlamamış görünə bilərdi. Belə dizayn test discovery-ni, failure yerini və səbəbini qarışdırır.

Bu layihənin hazırkı davranışı fərqlidir: `new ApiClient()` yalnız authenticated client obyektini qurur; constructor HTTP authentication göndərmir. Bearer token `ApiClient`-in ilk authenticated request-i zamanı `ApiAuthProvider.getToken()` vasitəsilə alınır və JVM daxilində cache edilir. `ApiClient.publicClient()` auth-suz client qaytarır. Buna görə missing credential əsasən authenticated request və ya `ConfigReader.getLogin()/getPassword()` çağırılan axında üzə çıxır.

### 6.2. `ConfigReader`-in real davranışı

`ConfigReader.getLogin()` və `getPassword()` yalnız `System.getenv(...)` vasitəsilə Maven prosesinin environment-ını oxuyur. Dəyər boş və ya yoxdursa `IllegalStateException` verir və credential dəyərlərini mesajda göstərmir.

Environment seçimi `-Denv=<ad>`, sonra `BAKU_ELECTRONICS_ENV`, sonra `default` ardıcıllığı ilədir. URL-lər üçün environment-specific system property, environment variable, ümumi property/environment variable, sonra yalnız `default` mühiti üçün kodda olan default istifadə edilir. Repository-də QA/staging host avtomatik verilmir.

### 6.3. Dəyişənlərin təhlükəsiz mövcudluq yoxlanması

Aşağıdakı nümunə dəyəri çap etmir:

```powershell
if ([string]::IsNullOrWhiteSpace($env:BAKU_ELECTRONICS_LOGIN)) {
    Write-Host 'BAKU_ELECTRONICS_LOGIN təyin edilməyib.'
} else {
    Write-Host 'BAKU_ELECTRONICS_LOGIN mövcuddur.'
}

if ([string]::IsNullOrWhiteSpace($env:BAKU_ELECTRONICS_PASSWORD)) {
    Write-Host 'BAKU_ELECTRONICS_PASSWORD təyin edilməyib.'
} else {
    Write-Host 'BAKU_ELECTRONICS_PASSWORD mövcuddur.'
}
```

Eyni layihə yoxlaması üçün real script:

```powershell
.\scripts\Test-ConfiguredCredentials.ps1
if ($LASTEXITCODE -eq 0) {
    Write-Host 'Credential preflight uğurludur.'
} else {
    Write-Host 'Credential preflight uğursuzdur; authenticated test işə salınmamalıdır.'
}
```

Script yalnız Process environment-də `BAKU_ELECTRONICS_LOGIN` və `BAKU_ELECTRONICS_PASSWORD` dəyişənlərini yoxlayır, mövcud olduqda dəyərləri çap etmir və missing olduqda exit code `1` qaytarır. User-level dəyişən diskdə təyin edilmiş olsa da, cari PowerShell prosesinə ötürülməyibsə script onu görməyə bilər.

`$env:BAKU_ELECTRONICS_LOGIN` və ya password dəyişənini birbaşa yazdırmaq təhlükəsiz diaqnostika deyil — dəyər terminal history, ekran görüntüsü və logda qala bilər.

### 6.4. Cari proses və daimi User Environment Variables fərqi

Yalnız cari PowerShell prosesi üçün təyin etmək nümunəsi aşağıdakı formadadır; literal secret-i README-yə və ya paylaşılmış history-yə yazmayın:

```powershell
$env:BAKU_ELECTRONICS_LOGIN = Read-Host 'Login'
$securePassword = Read-Host 'Password' -AsSecureString
$env:BAKU_ELECTRONICS_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
```

Bu dəyərlər həmin PowerShell və onun child process-ləri üçün keçərlidir. Terminal bağlandıqda və ya yeni terminal açıldıqda avtomatik daşınmır.

Daimi user-level konfiqurasiya üçün GUI istifadə edin:

1. `Win + R` → `sysdm.cpl`.
2. `Advanced` → `Environment Variables`.
3. `User variables for <user>` altında `New...` seçin.
4. `BAKU_ELECTRONICS_LOGIN` və `BAKU_ELECTRONICS_PASSWORD` adlarını ayrı-ayrı əlavə edin.
5. Dəyərləri yalnız Windows dialoquna daxil edin.
6. Açıq PowerShell-i və IntelliJ IDEA-nı yenidən başladın.
7. Yuxarıdakı mövcudluq yoxlamasını işlədin.

Parolun literal dəyərini `setx`, `Write-Host`, README, log və command line-a daxil etmək tövsiyə edilmir. `setx` istifadə edilərsə dəyəri history-yə və proseslərə sızdırmaq riski var; bu README-də literal `setx` nümunəsi verilmir.

### 6.5. Authentication axını və diaqnostika

`ApiAuthProvider`:

1. login/password oxuyur;
2. login-i Azərbaycan mobil nömrəsi formatına normallaşdırır (email login UI üçün ayrıca dəstək var);
3. RedID authorize URL-ə POST göndərir;
4. uğurlu JSON cavabından `data.code` götürür;
5. SSO login URL-ə `code` və `lang=az` göndərir;
6. `sso.token` alır.

RedID authorize və SSO URL-ləri `ConfigReader`-in default-larıdır. Authentication uğursuzluğunda səbəb yalnız credential deyil: server statusu, şəbəkə/DNS/TLS, API dəyişiklikləri, hesabın bloklanması və RedID/SSO xidmətinin vəziyyəti də mümkün səbəblərdir. Kod həssas response sahələrini maskalamağa çalışır, amma bütün logları paylaşmadan əvvəl yoxlayın.

Diaqnostika sırası:

1. Eyni terminalda `Test-ConfiguredCredentials.ps1` işlədin.
2. `mvn -version` ilə Maven JVM-ni yoxlayın.
3. Authenticated testin dəqiq sinif adını və endpoint-ini yoxlayın.
4. Authentication logunda yalnız status, URL kateqoriyası və stack trace-i paylaşın; token, code, password, cookie və login-i paylaşmayın.
5. `401/403` üçün credential/hesab/icazə; `429` üçün rate limit; `5xx` üçün server/endpoint vəziyyətini araşdırın.
6. Düzgün credential ilə də problem qalırsa, bu artıq avtomatik olaraq Java/Maven problemi sayılmamalıdır.

## 7. Gündəlik istifadə üçün əsas komandalar

Bu bölmədəki komandalar lokal fayllara və ya test seçiminə təsir edir; xarici hosta müraciət edən komandalar ayrıca qeyd olunur.

```powershell
Set-Location -LiteralPath 'C:\Users\OMAR\Bakuelectronics.az'
Get-Location
Test-Path -LiteralPath '.\pom.xml' -PathType Leaf
Get-ChildItem -Force
rg --files -g '!target' -g '!node_modules' | Sort-Object
java -version
javac -version
mvn -version
mvn -q -DskipTests test-compile
```

`test-compile` xarici API çağırmır; dependency download ilk dəfə internet/Maven repository çıxışı tələb edə bilər. `mvn test` və suite əmrləri isə test kodundan asılı olaraq canlı UI/API çağırışları edə bilər.

## 8. Bütün terminal komandalarının kataloqu

Aşağıdakı kataloq repository-də real fayllara və ya `pom.xml`-də real pluginlərə əsaslanır. Hər sətirdə risk ayrıca qeyd olunur: `Lokal` yalnız lokal yoxlamadır, `Xarici API/UI` canlı hosta müraciət edə bilər, `Hesab` isə credential və ya basket/session vəziyyətinə təsir edə bilər.

### A. Qovluq və fayl əməliyyatları

| Əmr | Məqsəd / uğur əlaməti | Tipik növbəti addım | Təsir |
|---|---|---|---|
| `Set-Location -LiteralPath 'C:\Users\OMAR\Bakuelectronics.az'` | Layihə kökünə keçir; `Get-Location` yolu təsdiqləyir | Yol səhvdirsə `Test-Path` və real checkout yolunu yoxlayın | Lokal |
| `Get-Location` | Cari qovluğu göstərir | `pom.xml` görünmürsə kökə keçin | Lokal |
| `Get-ChildItem -Force` | Kök faylları/qovluqları göstərir | `pom.xml`, `src`, suite XML-lərini axtarın | Lokal |
| `rg --files -g '!target' -g '!node_modules'` | Mənbə və konfiqurasiya fayllarını siyahıya alır | Gözlənən fayl yoxdursa adı uydurmayın | Lokal |
| `Test-Path -LiteralPath '.\pom.xml' -PathType Leaf` | Konkret faylın mövcudluğunu yoxlayır | `False` olduqda səhv qovluq və ya checkout araşdırılır | Lokal |
| `Get-Content -Raw -LiteralPath '.\pom.xml'` | Maven modelini oxuyur | Plugin/dependency adlarını real POM-dan götürün | Lokal |

### B. Java və Maven diaqnostikası

| Əmr | Nə vaxt / uğur | Tipik xəta və növbəti addım | Təsir |
|---|---|---|---|
| `java -version` | Java runtime görünür | `java is not recognized`: PATH/JDK | Lokal |
| `javac -version` | Compiler görünür | `javac` yoxdur: JRE əvəzinə JDK və PATH yoxlayın | Lokal |
| `$env:JAVA_HOME` | JDK kökünü göstərir | Boş və ya `bin`-ə işarə edirsə düzəldin | Lokal |
| `Get-Command java,javac,mvn -ErrorAction SilentlyContinue` | Faktiki executable yolunu göstərir | Gözlənilməyən shim/IDE yolu varsa PATH/IDE ayarını yoxlayın | Lokal |
| `($env:Path -split ';' | Where-Object { $_ -match 'Java|jdk|maven|Maven' })` | PATH-də uyğun entry-ləri göstərir | Köhnə entry əvvəl gəlirsə yeni proses açın | Lokal |
| `mvn -version` | Maven versiyası və istifadə etdiyi Java görünür | Maven başqa Java istifadə edirsə `JAVA_HOME`, PATH, IntelliJ Runner | Lokal |
| `mvn dependency:tree` | Dependency graph-ı göstərir | Resolve/conflict üçün repository, proxy, cache və POM yoxlayın | Dependency download ola bilər |
| `mvn help:effective-pom -Doutput=target/effective-pom.xml` | Effective Maven modelini fayla yazır | Plugin/property inheritance-i müqayisə edin | Lokal fayl yaradır |

### C. Maven build komandaları

```powershell
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
mvn -q -Dmaven.test.skip=true package
mvn -q -DskipTests test-compile -e
mvn -q -DskipTests test-compile -X
```

- `compile`: main source compile; `-DskipTests` testlərin icrasını dayandırır, main compile yenə baş verir.
- `test-compile`: test source-larını da compile edir, test metodlarını icra etmir.
- `-Dmaven.test.skip=true package`: test compile-ını da skip edərək package mərhələsinə keçir; test source dəyişikliklərinin compile yoxlaması üçün uyğun deyil.
- `-e`: Maven exception stack trace-i artırır.
- `-X`: çox ətraflı debug log verir; credential və token sızmasına qarşı logu paylaşmazdan əvvəl yoxlayın.

Uğur əlaməti `BUILD SUCCESS`; failure zamanı ilk konkret `Caused by`, `COMPILATION ERROR`, dependency və ya plugin sətrini ayırın. `target` yazılan build artefaktlarıdır; source kodu dəyişmir.

Layihədə POM-dan təsdiqlənən build pluginləri:

- `maven-surefire-plugin` `3.5.2` — TestNG və suite/group icrası;
- `allure-maven` `3.0.2` — Allure Maven report/serve goal-ları.

### D. TestNG və konkret testlərin icrası

Konkret class adları `src/test/java`-da təsdiqlənib:

```powershell
# Public, mutation etməyən API yoxlamaları
mvn -q '-Dtest=DefaultSearchResultTest,DefaultSearchResultContractTest,UnauthorizedRequestTest' test

# Bir UI smoke class-ı; canlı sayta browser açır
mvn -q '-Dtest=HomeSmokeTest' test

# Bir neçə class
mvn -q '-Dtest=HomeSmokeTest,CategoryNavigationTest,SearchTest' test

# Mövcud suite-lər
mvn -q '-Dsurefire.suiteXmlFiles=testng-parallel-ui-pilot.xml' test
mvn -q '-Dsurefire.suiteXmlFiles=testng-safe-full-run-with-integration.xml' test
mvn -q '-Dsurefire.suiteXmlFiles=testng.xml' test

# Mövcud group-lar
mvn -q '-Dgroups=smoke' test
mvn -q '-Dgroups=regression' test
mvn -q '-Dgroups=api' test
mvn -q '-Dgroups=integration' test
```

`-Dtest=...` Surefire-in class seçicisidir. `testng.xml` suite seçimi ilə eyni deyil: konkret class seçimi suite-də olmayan test sinfini də işə sala bilər. `-Dgroups=api` layihədəki bütün `api` qruplarını seçə bilər; bura auth və basket mutation testləri daxildir. `-Dgroups=regression` integration və login/cart testlərini də seçə bilər. Group-ları “təhlükəsiz” hesab etməzdən əvvəl annotasiyaları yoxlayın.

Uğursuz test üçün:

```powershell
mvn '-Dtest=HomeSmokeTest' test -e
$LASTEXITCODE
```

`BUILD FAILURE` və non-zero `$LASTEXITCODE` test/build problemini göstərir; səbəb stack trace və test report-larında araşdırılmalıdır.

### E. Credential tələb edən testlər

```powershell
.\scripts\Test-ConfiguredCredentials.ps1
if ($LASTEXITCODE -eq 0) {
    mvn -q '-Dtest=AuthenticationFlowTest' test
}
```

Bu əmrlə `AuthenticationFlowTest` RedID/SSO auth axınını canlı endpoint-lərə göndərir. `LoginTest` credential ilə UI login edir:

```powershell
.\scripts\Test-ConfiguredCredentials.ps1
if ($LASTEXITCODE -eq 0) {
    mvn -q '-Dtest=LoginTest' test
}
```

Əvvəl preflight failure olarsa Maven auth testini işə salmayın. Preflight keçsə də server, hesab və API problemləri mümkündür. Bu əmrlər credentials dəyərini çap etməməlidir; Maven debug logunu paylaşmazdan əvvəl yoxlayın.

### F. Selenium və UI testləri

Mövcud UI class-lar:

- `HomeSmokeTest`, `CategoryNavigationTest`, `SearchTest`;
- `NegativeSearchTest`, `PaginationTest`, `ProductBrandFilterTest`, `ProductPriceFilterTest`, `ProductSortingTest`;
- `ProductTest`, `ProductDetailsValidationTest`;
- `LoginSmokeTest`, `LoginTest`;
- `AddToCartTest`, `CartSmokeTest`, `CartQuantityIncreaseTest`, `CartQuantityDecreaseTest`, `CartRemoveProductTest`.

Nümunə:

```powershell
mvn -q '-Dtest=HomeSmokeTest' test
mvn -q '-Dtest=CategoryNavigationTest,SearchTest' test
mvn -q '-Dtest=NegativeSearchTest' test
```

`NegativeSearchTest` `src/test/resources/testdata/negative-search-data.json` faylındakı 3 query-ni DataProvider ilə işləyir. `BaseTest` hər method-dan əvvəl ChromeDriver yaradır, uğursuz UI testində screenshot almağa çalışır və sonra driver-i bağlayır. Screenshot yolu `target/test-output/screenshots`-dır.

Browser açılmırsa əvvəlcə faktiki stack trace-i, Chrome quraşdırmasını, WebDriverManager download çıxışını, PATH/proxy/DNS vəziyyətini və driver session mesajını yoxlayın. Chrome və WebDriver versiyalarını dəyişmək ilkin avtomatik həll deyil.

Element tapılmırsa URL, səhifənin yüklənməsi, popup, locator, timeout, stale element və canlı sayt dəyişikliklərini ayırın. `target` log/screenshot-ları və Selenium exception-un ilk səbəb sətrini müqayisə edin.

### G. API testləri

Koddan təsdiqlənən default API base URL:

```text
https://5vyra9x.bakuelectronics.az
```

| Endpoint | HTTP | Real istifadə |
|---|---|---|
| `/default-search-result` | GET | `DefaultSearchResultTest`, `DefaultSearchResultContractTest`, məhsul service/integration |
| `/addbasket` | POST | add, response fields, invalid slug/quantity, unauthorized |
| `/updateBasket` | PUT | `UpdateBasketTest` |
| `/basket-remove-product` | DELETE | delete və cleanup |
| RedID authorize URL | POST | `ApiAuthProvider`, `AuthenticationFlowTest` |
| SSO login URL | POST | `ApiAuthProvider`, `AuthenticationFlowTest` |

API class-lar:

- Public: `DefaultSearchResultTest`, `DefaultSearchResultContractTest`;
- Authenticated/mutation və ya auth tələb edən: `AddBasketTest`, `UpdateBasketTest`, `DeleteBasketTest`, `BasketResponseFieldsTest`, `InvalidProductSlugTest`, `InvalidQuantityTest`;
- Auth-suz rejection: `UnauthorizedRequestTest` (`401` və ya `403` gözləyir);
- ayrıca auth flow: `AuthenticationFlowTest`.

`ApiTestReporter` status, JSON content type/root, field tipi/dəyəri, basket contract və response time (`< 5000 ms`) yoxlamaları edir. `ApiClient.patch()` helper-i mövcuddur, lakin testlərdə təsdiqlənmiş PATCH endpoint/test yoxdur; buna görə README PATCH icrası təklif etmir.

Public API smoke üçün:

```powershell
mvn -q '-Dtest=DefaultSearchResultTest,DefaultSearchResultContractTest' test
```

Unauthorized request canlı API-yə auth-suz POST göndərir, lakin düzgün credential oxumur:

```powershell
mvn -q '-Dtest=UnauthorizedRequestTest' test
```

Bu, xarici API-yə read/write request-dir; response serverin cari davranışından asılıdır.

### H. Allure və test nəticələri

`src/test/resources/allure.properties`:

```properties
allure.results.directory=target/allure-results
```

Nəticələri yoxlamaq:

```powershell
Test-Path -LiteralPath '.\target\allure-results' -PathType Container
Get-ChildItem -LiteralPath '.\target\allure-results' -ErrorAction SilentlyContinue
Get-ChildItem -LiteralPath '.\target\test-output\screenshots' -ErrorAction SilentlyContinue
```

Repository-də lokal Allure CLI `.allure\allure-2.39.0\bin\allure.bat` kimi mövcuddur; icra etməzdən əvvəl yolunu yoxlayın:

```powershell
Test-Path -LiteralPath '.\.allure\allure-2.39.0\bin\allure.bat' -PathType Leaf
```

Real plugin və lokal CLI variantları:

```powershell
mvn allure:report
mvn allure:serve
.\.allure\allure-2.39.0\bin\allure.bat generate target/allure-results -o target/allure-report --clean
.\.allure\allure-2.39.0\bin\allure.bat open target/allure-report
```

`mvn allure:report` Maven plugin-in report goal-udur; nəticə adətən `target/site/allure-maven-plugin/index.html` altında olur. `allure:serve` lokal server açmağa çalışır. CLI `generate` yalnız output report qovluğuna `--clean` tətbiq edir, lakin `target/allure-results` daxilindəki əvvəlki raw nəticələri silmir.

Raw nəticələr müxtəlif icralardan yığıla bildiyi üçün köhnə və yeni testləri ayırmaq lazımdır. `target/allure-results`-i təmizləyən əmri bu layihədə standart script kimi təsdiqlənməyib; nəticələri silən `Remove-Item` əmri avtomatik istifadə etməyin. Mövcud nəticələr qorunmalıdır.

### I. PowerShell exit code və şərti icra

```powershell
mvn -q -DskipTests test-compile
$exitCode = $LASTEXITCODE
if ($exitCode -eq 0) {
    Write-Host 'Maven compile uğurludur.'
} else {
    Write-Host "Maven compile uğursuzdur. Exit code: $exitCode"
}
```

Credential preflight + test:

```powershell
.\scripts\Test-ConfiguredCredentials.ps1
if ($LASTEXITCODE -eq 0) {
    mvn -q '-Dtest=AuthenticationFlowTest' test
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'AuthenticationFlowTest uğursuz oldu.'
    }
} else {
    Write-Host 'Credential dəyişənləri cari process-də mövcud deyil.'
}
```

`$LASTEXITCODE` son native proqramın exit code-udur; növbəti native əmrdən sonra dəyişə bilər, buna görə dərhal lokal dəyişənə saxlayın. `Test-ConfiguredCredentials.ps1` üçün exit code `0` present, `1` missing deməkdir.

### J. Səbət və integration testləri

Səbətə və ya hesab session-ına toxunan testlər:

- API: `AddBasketTest`, `UpdateBasketTest`, `DeleteBasketTest`, `BasketResponseFieldsTest`, `InvalidQuantityTest`;
- integration: `BasketApiToUiIntegrationTest`;
- UI: `AddToCartTest`, `CartSmokeTest`, `CartQuantityIncreaseTest`, `CartQuantityDecreaseTest`, `CartRemoveProductTest`.

Bu testlər authenticated request, canlı sayt session-ı və ya real səbət vəziyyəti ilə işləyə bilər. API testlərinin bir hissəsində `BasketTestCleanup`/`BasketTestSupport` ilə lock və silmə var, amma cleanup xarici request-in uğurundan asılıdır. UI cart testləri üçün ümumi API cleanup mexanizmi yoxdur. Cleanup failure-dan sonra real basket state qala bilər.

İstifadəçinin açıq razılığı, ayrıca test hesabı və cleanup planı olmadan aşağıdakı əmrləri işə salmayın:

```powershell
mvn -q '-Dtest=AddBasketTest' test
mvn -q '-Dtest=UpdateBasketTest,DeleteBasketTest' test
mvn -q '-Dtest=AddToCartTest,CartSmokeTest,CartQuantityIncreaseTest,CartQuantityDecreaseTest,CartRemoveProductTest' test
mvn -q '-Dtest=BasketApiToUiIntegrationTest' test
```

`testng.xml`, `-Dgroups=api` və `-Dgroups=regression` geniş seçimləri mutation testlərini də qoşa bilər. Bu seçimləri təhlükəsiz suite kimi təqdim etmək olmaz.

## 9. UI testləri

| Test class | Group | Əhatə / risk |
|---|---|---|
| `HomeSmokeTest` | `smoke` | Ana səhifə |
| `CategoryNavigationTest` | `smoke` | `Smartfonlar` kateqoriyası |
| `SearchTest` | `smoke` | Axtarış və gözlənilən məhsul |
| `NegativeSearchTest` | `regression` | 3 negative search DataProvider invocation-ı |
| `PaginationTest` | `regression` | Pagination |
| `ProductBrandFilterTest` | `regression` | Brand filter |
| `ProductPriceFilterTest` | `regression` | Qiymət aralığı |
| `ProductSortingTest` | `regression` | Qiymət sıralaması |
| `ProductTest` | `regression` | Product route/details/cart button |
| `ProductDetailsValidationTest` | `regression` | Product name/code/price/characteristics |
| `LoginSmokeTest` | `smoke` | RedID səhifəsi və login form tab-ları; credential tələb etmir |
| `LoginTest` | `regression` | Credential ilə login, account panel və logout |
| `AddToCartTest` | `smoke` | Səbətə əlavə; mutation |
| `CartSmokeTest` | `regression` | Səbət; hesab/session riski |
| `CartQuantityIncreaseTest` | `regression` | Quantity artırma; mutation |
| `CartQuantityDecreaseTest` | `regression` | Quantity azaltma; mutation |
| `CartRemoveProductTest` | `regression` | Məhsul silmə; mutation |

## 10. API testləri

| Test class | Group | Auth/mutation vəziyyəti |
|---|---|---|
| `DefaultSearchResultTest` | `api` | Public GET |
| `DefaultSearchResultContractTest` | `api` | Public GET contract |
| `UnauthorizedRequestTest` | `api` | Auth-suz POST; server `401/403` gözləntisi |
| `AuthenticationFlowTest` | `api` | RedID + SSO; credential tələb edir |
| `InvalidProductSlugTest` | `api` | Authenticated `/addbasket`-ə invalid slug |
| `InvalidQuantityTest` | `api` | Authenticated `/addbasket` quantity `0`; cleanup ehtimalı |
| `AddBasketTest` | `api` | Authenticated basket add |
| `UpdateBasketTest` | `api` | Add + PUT update + cleanup |
| `DeleteBasketTest` | `api` | Add + DELETE |
| `BasketResponseFieldsTest` | `api` | Basket add/response contract |

Bu 10 API test sinfi source-da mövcuddur. `testng.xml` onlardan `AuthenticationFlowTest`-i seçmir; əsas suite-lərin faktiki siyahısı növbəti bölmədədir.

## 11. TestNG suite və qrupları

Mövcud XML faylları:

| Fayl | Faktiki class-lar | Xüsusiyyət |
|---|---|---|
| `testng.xml` | 17 UI + 2 integration + 9 API | Serial default; retry transformer + Allure listener; auth flow/support test seçilmir |
| `testng-safe-full-run-with-integration.xml` | 10 UI + 1 integration + 3 API | Serial; login mutation və basket mutation class-ları çıxarılıb; `LoginSmokeTest` qalır |
| `testng-parallel-ui-pilot.xml` | `HomeSmokeTest`, `CategoryNavigationTest`, `SearchTest` | `parallel="classes"`, `thread-count="2"`; retry transformer yoxdur |

Suite icrası:

```powershell
mvn -q '-Dsurefire.suiteXmlFiles=testng-safe-full-run-with-integration.xml' test
```

Bu suite integration içində yalnız `ProductApiUiIntegrationTest` seçir; o da canlı API və UI-yə müraciət edir. Tam `testng.xml` bütün seçilmiş API/UI/integration class-larını işə salır və riskli ola bilər.

Source-da qruplar:

- `smoke`: 4 UI class (`AddToCartTest` səbət mutation edir);
- `regression`: UI regression class-ları, hər iki integration class və `LoginTest`;
- `api`: 10 API test class-ı;
- `integration`: 2 integration class.

`WaitUtilsTest`-in `@Test` annotasiyasında group yoxdur və XML suite-lərdə seçilmir. `AuthenticationFlowTest` `api` qrupundadır, amma XML suite-lərdə seçilmir.

Test sayının audit nəticəsi: 17 UI + 10 API + 2 integration + 1 support = 30 `@Test` metodu. `NegativeSearchTest` 3 DataProvider invocation yaradır; retry əlavə cəhdləri bu saya daxil deyil.

## 12. Səbət və integration testləri

`ProductApiUiIntegrationTest` public API-dən məhsul seçir, UI axtarışında həmin məhsulu tapır və product slug/code/name/model/price məlumatlarını müqayisə edir. Bu test canlı API və UI-yə read request-lər göndərir.

`BasketApiToUiIntegrationTest` authenticated API ilə məhsulu səbətə əlavə edir, eyni hesabla UI login edir və səbətdə slug/quantity yoxlayır. Bu test həm credential, həm canlı API/UI, həm də real basket state riski daşıyır.

Integration testlərini seçmək üçün `-Dgroups=integration` realdır, lakin təhlükəsiz deyil:

```powershell
mvn -q '-Dgroups=integration' test
```

İstifadəçi razılığı olmadan bu komanda icra edilməməlidir.

## 13. Allure hesabatları və loglar

Allure nəticəsi `target/allure-results`-da, failure screenshot-ları `target/test-output/screenshots`-da saxlanır. `BaseTest` failure zamanı screenshot yaratmağa və Allure attachment etməyə çalışır; WebDriver session bağlıdırsa screenshot olmaya bilər.

Report yaranmırsa:

1. Testin həqiqətən icra olunduğunu və `target/allure-results`-da yeni fayl vaxtlarını yoxlayın.
2. `allure.properties`-də output yolunu yoxlayın.
3. `mvn allure:report` outputunu və `target/site/allure-maven-plugin` qovluğunu yoxlayın.
4. Lokal CLI yolunun mövcudluğunu `Test-Path` ilə yoxlayın.
5. `target/allure-results`-in köhnə nəticələrlə qarışa biləcəyini nəzərə alın.

Heç bir nəticəni silməyin; `target/allure-results` təmizlənməsi əvvəlki nəticələri itirə bilər. `allure generate ... --clean` output report qovluğunu təmizləyir, raw results qovluğunu yox.

## 14. Problemlərin həlli — addım-addım

### 14.1. Java tapılmır

- Əlamət: `java is not recognized` və ya `javac` yoxdur.
- Təhlükəsiz yoxlama: `Get-Command java,javac`; `java -version`; `javac -version`.
- Ehtimal: JDK quraşdırılmayıb və ya PATH səhvdir.
- Həll: JDK quraşdırmasını/PATH-ı düzəldin, yeni terminal açın.
- Doğrulama: hər iki versiya görünür və `javac` compiler-dir.
- Nəticə alınmırsa: `JAVA_HOME` və faktiki executable yolunu yoxlayın.

### 14.2. `JAVA_HOME` yanlışdır

- Əlamət: Maven Java tapmır və ya `JAVA_HOME is not defined correctly`.
- Yoxlama: `$env:JAVA_HOME`; `Test-Path "$env:JAVA_HOME\bin\java.exe"`.
- Ehtimal: JDK əvəzinə `bin`/JRE yolu.
- Həll: User/System Environment Variables-da JDK kökünü verin və yeni proses açın.
- Doğrulama: `mvn -version`.

### 14.3. Maven tapılmır və ya başqa Java ilə işləyir

- Əlamət: `mvn` tanınmır və ya `mvn -version` Java 23 göstərmir.
- Yoxlama: `Get-Command mvn`; `mvn -version`; PATH entry-ləri.
- Ehtimal: Maven PATH-da deyil, başqa Java əvvəl gəlir və ya IntelliJ Runner ayrıdır.
- Həll: Maven home/PATH və IDE Maven Runner JDK-sını düzəldin, yeni proses açın.
- Doğrulama: Maven və Java sətirləri uyğun görünür, sonra `mvn -q -DskipTests test-compile`.

### 14.4. Dependency yüklənmir

- Əlamət: `Could not resolve dependencies`, timeout, TLS/DNS/proxy.
- Yoxlama: `mvn dependency:tree`; POM versiyaları; Maven local repository/proxy.
- Ehtimal: repository şəbəkəsi, mirror, proxy və ya transient xidmət problemi.
- Həll: şəbəkə/proxy ayarını yoxlayın; POM dependency-sini sübutsuz dəyişməyin.
- Doğrulama: `mvn -q -DskipTests test-compile` → `BUILD SUCCESS`.

### 14.5. Layihə compile olunmur

- Əlamət: `COMPILATION ERROR`.
- Yoxlama: `mvn -q -DskipTests test-compile -e`.
- Ehtimal: Java source/target uyğunsuzluğu, syntax/import xətası və ya dependency.
- Həll: ilk compiler error-u, JDK və POM-u müqayisə edin.
- Doğrulama: eyni compile əmri uğurlu olmalıdır.
- README əsasında source/pom dəyişdirməyin; real sübut olmadan kod dəyişikliyi təklif etməyin.

### 14.6. TestNG testləri tapmır

- Əlamət: `No tests were executed` və ya gözlənən class seçilmir.
- Yoxlama: class adını `rg "class ..." src/test/java` ilə yoxlayın; `mvn -q -DskipTests test-compile`; suite XML-ni oxuyun.
- Ehtimal: səhv class adı, suite-də class yoxdur, group annotasiyası yoxdur və ya `-Dtest` ilə suite qarışdırılıb.
- Həll: source-da real class adını, `@Test` və group-u istifadə edin.
- Doğrulama: Surefire summary-da seçilən test görünür.

### 14.7. Test sinfi yaradılarkən authentication xətası

- Əlamət: failure test method-dan əvvəl görünür.
- Yoxlama: source-da constructor/field initializer və `ApiClient` davranışını oxuyun; preflight script işlədin.
- Faktiki vəziyyət: bu layihədə `ApiClient` constructor HTTP auth göndərmir; token ilk authenticated request-də alınır.
- Ehtimal: `ConfigReader` çağırışı, static init, TestNG setup və ya ilk authenticated request.
- Həll: credential preflight, Maven prosesinin environment-ı və stack trace.
- Doğrulama: public testlər auth olmadan, authenticated testlər preflight-dan sonra seçilir.

### 14.8. Environment variable tapılmır

- Əlamət: `Authenticated test requires ... to be set in the Maven process environment`.
- Yoxlama: dəyəri çap etmədən yuxarıdakı `if` blokları və `.\scripts\Test-ConfiguredCredentials.ps1`.
- Ehtimal: dəyişən başqa terminalda təyin edilib, IDE köhnə prosesdir və ya User variable yeni process-ə keçməyib.
- Həll: eyni terminalda Process variable təyin edin və ya Windows GUI-dən sonra terminal/IDE-ni yenidən açın.
- Doğrulama: script exit code `0`, sonra konkret authenticated test.

### 14.9. API authentication uğursuz olur

- Əlamət: `API authentication failed`, RedID/SSO error və ya token missing.
- Yoxlama: preflight, endpoint URL config, status və sanitized log; token/code/password paylaşmayın.
- Ehtimal: credential, format, account, network, RedID/SSO server və API contract.
- Həll: səbəbi status mərhələsinə görə ayırın; yalnız bundan sonra account/server owner ilə yoxlayın.
- Doğrulama: auth flow və bir authenticated request uğurlu olmalıdır.

### 14.10. API `401/403`, `429` və ya `5xx` qaytarır

- `401/403`: credential/token, icazə, endpoint və auth header; `UnauthorizedRequestTest` üçün `401/403` gözləniləndir.
- `429`: rate limit; təkrarları artırmayın, interval və server limitini araşdırın.
- `5xx`: server/upstream; bunu avtomatik olaraq test kodu xətası saymayın.
- Yoxlama: konkret test, status line, content type və təhlükəsiz response summary.
- Doğrulama: eyni endpoint/mühitdə status və JSON contract yenidən uyğun olmalıdır.

### 14.11. Selenium browser-i açmır

- Əlamət: ChromeDriver/session creation exception.
- Yoxlama: Chrome quraşdırması, `mvn -version`, WebDriverManager logu, network/proxy və stack trace.
- Həll: əvvəlcə səbəbli xətanı düzəldin; browser/driver versiyasını kor-koranə dəyişməyin.
- Doğrulama: yalnız `HomeSmokeTest` kimi bir UI testini seçərək yoxlayın.

### 14.12. Element tapılmır və ya klikləmə uğursuz olur

- Əlamət: timeout, stale element, click intercepted və ya görünməyən element.
- Yoxlama: URL, popup, locator, səhifə state-i və screenshot.
- Ehtimal: canlı DOM dəyişib, popup üstünü örtür, səhifə gecikir və ya səhv route-dur.
- Həll: faktiki DOM/URL və exception-a əsasən araşdırın; test locator-unu sübutsuz dəyişməyin.
- Doğrulama: eyni konkret testdə element görünür və action tamamlanır.

### 14.13. Test keçir, amma Allure yaranmır

- Əlamət: `target/allure-results` boş/yoxdur və report açılmır.
- Yoxlama: `allure.properties`, raw result faylları, Maven report outputu və CLI `Test-Path`.
- Ehtimal: test icra olunmayıb, results başqa qovluqdadır, plugin goal işləməyib və ya köhnə/boş inputdur.
- Həll: test outputunu və plugin logunu yoxlayın; raw results silməyin.
- Doğrulama: yeni result faylları və report index yaranır.

### 14.14. Test lokalda keçir, başqa mühitdə uğursuz olur

- Yoxlama: `mvn -version`, `java/javac`, `-Denv`, URL config, browser, network/proxy və test data.
- Ehtimal: fərqli Java/Maven/browser, environment variable, host, rate limit, hesab vəziyyəti və canlı data.
- Həll: mühit fərqlərini cədvəl üzrə müqayisə edin; secret-i loglamayın.
- Doğrulama: eyni test, eyni suite və eyni konfiqurasiya ilə təkrar edin.

## 15. Təhlükəsizlik və secret-lərin qorunması

- Login, password, token, authorization code, cookie və session dəyəri README-yə, source-a, POM-a və loga yazılmır.
- Environment variable mövcudluğunu yoxlayın, dəyərini çap etməyin.
- Maven `-X` və authentication loglarını paylaşmazdan əvvəl redaktə edin.
- User Environment Variables yalnız şəxsi/local konfiqurasiya üçündür; paylaşılmış maşında secret idarəetmə siyasətinə əməl edin.
- Live API/UI və real hesab səbəti ilə işləyən testlər üçün ayrıca test hesabı və cleanup planı istifadə edin.
- `target/allure-results`, screenshot və loglarda həssas data ola bilər; paylaşmazdan əvvəl yoxlayın.
- `target/allure-results`-i və başqa artefaktları silən əmrlər bu README-də avtomatik prosedur deyil.

## 16. Tez-tez verilən suallar

**JDK 23.0.2 məcburidirmi?** Xeyr. `pom.xml` compiler source/target `23` tələb edir; konkret patch versiyasını `java`, `javac` və `mvn -version` ilə təsdiqləyin.

**Maven wrapper varmı?** Xeyr, `mvnw.cmd` və `mvnw` repository-də yoxdur.

**`LoginSmokeTest` credential tələb edirmi?** Mənbəyə görə RedID səhifəsi və form tab-larını yoxlayır, `ConfigReader.getLogin()` çağırmır. `LoginTest` isə credential tələb edir.

**`-Dgroups=api` təhlükəsizdirmi?** Xeyr. `api` qrupunda authentication və basket mutation testləri də var. Public testləri konkret `-Dtest` ilə seçin.

**`testng-safe-full-run-with-integration.xml` tam təhlükəsizdirmi?** Xeyr. Basket mutation class-ları çıxarılsa da canlı UI/API və `ProductApiUiIntegrationTest` işləyir.

**`testng.xml` bütün testləri seçirmi?** Bütün source testlərini yox: `AuthenticationFlowTest` və `WaitUtilsTest` seçilmir; suite-də 17 UI, 2 integration və 9 API class var.

**Niyə `NegativeSearchTest` üçün bir class olsa da bir neçə invocation görünür?** DataProvider JSON-dakı 3 query-ni verir.

**API token constructor-da alınır?** Xeyr. `ApiClient` constructor yalnız request specification qurur; token ilk authenticated request-də alınır.

**Allure raw nəticələrini necə təmizləyim?** Bu README avtomatik silmə əmri vermir. Təmizləmə əvvəlki nəticələri itirə bilər və ayrıca təsdiq/prosedur tələb edir.

## 17. Audit və faktiki yoxlamalar

Bu README-ni hazırlamaq üçün real olaraq oxunan fayllar:

- `pom.xml`, `.gitignore`, `testng.xml`, `testng-parallel-ui-pilot.xml`, `testng-safe-full-run-with-integration.xml`;
- `scripts/Test-ConfiguredCredentials.ps1`;
- bütün `src/main/java` və `src/test/java` Java faylları;
- `src/test/resources/allure.properties` və `negative-search-data.json`;
- əvvəlki `README.md`.

Faktiki struktur yoxlamasında 17 UI, 10 API, 2 integration və 1 support test class/metod qrupu təsdiqləndi; `BaseTest` support base class kimi ayrıca `@Test` metodu deyil. Cəmi 30 `@Test` metodu var. `NegativeSearchTest` 3 DataProvider invocation yaradır.

Bu tapşırıq zamanı faktiki icra edilən lokal yoxlama komandalarına `rg --files`, `Get-ChildItem`, `Get-Content -Raw`, `Test-Path`, `Get-Command` və Allure CLI üçün `Test-Path` daxildir. `java -version`, `javac -version` və `mvn -version` də yoxlanmağa cəhd edilib, lakin bu mühitdə native process icazəsi/əlçatanlığı səbəbindən etibarlı versiya nəticəsi alınmayıb. `mvn test-compile`, public API testləri, credential-li testlər, basket mutation, integration, UI və tam suite testləri icra edilməyib. Allure report generasiya edilməyib və mövcud Allure nəticələri silinməyib.

Versiya komandalarının bu mühitdə native process kimi işlədilməsi ayrıca mühit icazəsi/əlçatanlıq problemi ilə nəticələnə bilər; README-də versiya nəticəsi uydurulmur. İstənilən istifadəçi `java -version`, `javac -version` və `mvn -version` komandalarını öz terminalında icra etməlidir.

## Quick Reference

```powershell
Set-Location -LiteralPath 'C:\Users\OMAR\Bakuelectronics.az'
java -version
javac -version
mvn -version
mvn -q -DskipTests test-compile

# Credential dəyərini göstərmədən preflight
.\scripts\Test-ConfiguredCredentials.ps1

# Public API contract yoxlamaları
mvn -q '-Dtest=DefaultSearchResultTest,DefaultSearchResultContractTest' test

# Auth-suz unauthorized contract
mvn -q '-Dtest=UnauthorizedRequestTest' test

# Məhdud UI pilot
mvn -q '-Dsurefire.suiteXmlFiles=testng-parallel-ui-pilot.xml' test

# Safe suite belə canlı hostlara müraciət edir
mvn -q '-Dsurefire.suiteXmlFiles=testng-safe-full-run-with-integration.xml' test

# Allure, yalnız result faylları olduqda
mvn allure:report
```

Bu qısa siyahı da canlı API/UI testlərini və credential/basket risklərini aradan qaldırmır. Səbətə toxunan komandaları yalnız açıq razılıq, test hesabı və cleanup planı ilə seçin.

