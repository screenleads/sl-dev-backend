# CI/CD Configuration Guide - ScreenLeads Backend

## 📋 Overview

This document describes the CI/CD infrastructure configured for the ScreenLeads backend.

## 🔄 GitHub Actions Workflows

### 1. CI Tests (`ci-tests.yml`)

**Triggers:**
- Push to `develop`, `main`, `master`
- Pull requests to these branches

**Steps:**
1. ✅ Checkout code
2. ☕ Setup JDK 17
3. 📦 Cache Maven dependencies
4. 🧪 Run all tests (`mvn clean test`)
5. 📊 Generate JaCoCo coverage report
6. 💬 Comment coverage on PRs
7. 📤 Upload coverage artifacts (30 days retention)
8. ✔️ Verify coverage threshold (minimum 35%)

**Configuration:**
```yaml
Coverage threshold: 35%
Artifact retention: 30 days
Java version: 17 (Temurin)
```

### 2. SonarQube Analysis (`sonarqube.yml`)

**Triggers:**
- Push to `develop`, `main`
- Pull requests to these branches

**Required Secrets:**
- `SONAR_TOKEN`: SonarQube authentication token
- `SONAR_HOST_URL`: SonarQube server URL (e.g., https://sonarcloud.io)

**Setup Instructions:**

1. **Create SonarQube project:**
   ```bash
   # En SonarQube/SonarCloud:
   # - Crear nuevo proyecto "screenleads_sl-dev-backend"
   # - Generar token de acceso
   ```

2. **Configure GitHub Secrets:**
   ```
   Repository Settings → Secrets and variables → Actions
   
   SONAR_TOKEN: [token generado en SonarQube]
   SONAR_HOST_URL: https://sonarcloud.io
   ```

3. **Quality Gates (recommended):**
   - Coverage on new code: ≥ 80%
   - Duplicated lines: ≤ 3%
   - Maintainability rating: A
   - Reliability rating: A
   - Security rating: A

### 3. Dependabot Auto-Merge (`dependabot-auto-merge.yml`)

**Behavior:**
- Automatically merges patch and minor updates
- Requires CI tests to pass
- Only for Dependabot PRs

**Manual Review Required:**
- Major version updates
- Security vulnerabilities (manual verification)

## 🤖 Dependabot Configuration

**Maven Dependencies:**
- Schedule: Weekly (Mondays at 9:00 AM)
- Open PRs limit: 10
- Labels: `dependencies`, `automated`
- Commit prefix: `deps`

**GitHub Actions:**
- Schedule: Weekly (Mondays)
- Labels: `github-actions`, `dependencies`
- Commit prefix: `ci`

**Ignored Updates:**
- Spring Boot major versions (manual migration required)
- Java major versions (manual migration required)

## 🎣 Git Hooks (Husky)

### Pre-commit Hook

**Location:** `.husky/pre-commit`

**Actions:**
- Runs tests for modified test files
- Prevents commit if tests fail

**Setup (first time):**
```bash
# Instalar Husky (si no está instalado)
npm install husky --save-dev
npx husky install

# Dar permisos de ejecución (Linux/Mac)
chmod +x .husky/pre-commit
```

**Disable temporarily:**
```bash
# Usar --no-verify para saltar hooks
git commit --no-verify -m "mensaje"
```

## 📊 Coverage Reports

### Local Development
```bash
# Generar reporte
mvn clean test jacoco:report

# Abrir reporte
start target/site/jacoco/index.html  # Windows
open target/site/jacoco/index.html   # Mac
xdg-open target/site/jacoco/index.html  # Linux
```

### GitHub Actions
- Reports uploaded as artifacts (30 days)
- Accessible in: Actions → Workflow run → Artifacts → `jacoco-report`

### SonarQube
- Detailed analysis at configured `SONAR_HOST_URL`
- Historical trends and code quality metrics

## 🚀 First-Time Setup

### 1. Enable GitHub Actions
```bash
# Las workflows se activan automáticamente al hacer push
git add .github/
git commit -m "ci: add GitHub Actions workflows"
git push origin develop
```

### 2. Configure SonarQube
```bash
# 1. Crear proyecto en SonarQube/SonarCloud
# 2. Copiar project key: screenleads_sl-dev-backend
# 3. Generar token
# 4. Agregar secrets en GitHub:
#    - SONAR_TOKEN
#    - SONAR_HOST_URL
```

### 3. Enable Dependabot
```bash
# Dependabot se activa automáticamente al detectar dependabot.yml
# Verificar en: Repository Settings → Security → Dependabot
```

### 4. Setup Husky (Local)
```bash
# En tu máquina local
cd sl-dev-backend
npm install  # Si tienes package.json
npx husky install

# Windows: Asegurar ejecución de scripts
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

## ⚙️ Customization

### Adjust Coverage Threshold
```yaml
# .github/workflows/ci-tests.yml (línea 51)
if [ "$COVERAGE" -lt 35 ]; then  # Cambiar 35 a tu threshold deseado
```

### Change Test Schedule
```yaml
# .github/dependabot.yml
schedule:
  interval: "daily"  # Opciones: daily, weekly, monthly
  day: "monday"      # Solo para weekly
  time: "09:00"
```

### Modify Auto-Merge Rules
```yaml
# .github/workflows/dependabot-auto-merge.yml (línea 20)
if: |
  steps.metadata.outputs.update-type == 'version-update:semver-patch'
  # Agregar más condiciones aquí
```

## 🔧 Troubleshooting

### Tests failing in CI but passing locally
```bash
# Verificar versión de Java
java -version  # Debe ser 17

# Limpiar cache de Maven
mvn clean
rm -rf ~/.m2/repository
```

### SonarQube analysis failing
```bash
# Verificar secrets configurados
# GitHub → Settings → Secrets → Actions
# Debe existir: SONAR_TOKEN, SONAR_HOST_URL

# Verificar project key en workflow
grep "sonar.projectKey" .github/workflows/sonarqube.yml
```

### Husky hooks not running
```bash
# Reinstalar hooks
npx husky install
chmod +x .husky/pre-commit  # Linux/Mac

# Windows: Verificar Git configuration
git config core.hooksPath
# Debe mostrar: .husky
```

## 📈 Metrics to Monitor

- ✅ Test success rate (target: 100%)
- 📊 Coverage trend (target: ≥35%, objetivo 60%)
- 🐛 SonarQube issues (target: 0 blockers, 0 critical)
- 🔄 Dependency freshness (target: <30 days old)
- ⏱️ CI execution time (target: <5 minutes)

## 🎯 Next Steps

1. **Week 1:** Monitor CI/CD stability, fix any failures
2. **Week 2:** Configure SonarQube quality gates
3. **Week 3:** Review Dependabot PRs, adjust ignore rules
4. **Week 4:** Optimize CI performance (caching, parallelization)

---
*Última actualización: 2025-01-04*
