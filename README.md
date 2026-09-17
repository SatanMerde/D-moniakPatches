# 🦈 D-moniak Patches

Bundle de patchs [Morphe](https://github.com/morpheapp) créé par **SatanMerde**, incluant un patch dédié à **Hungry Shark World** (`com.ubisoft.hungrysharkworld`).

## ❓ À propos / About

Ce projet fournit des patchs modulaires pour applications Android utilisant le framework Morphe.

Le premier patch intégré permet de **débloquer et réclamer instantanément toutes les récompenses publicitaires** (réanimations de requin lors d'un Game Over, doublement des pièces et gemmes, coffres quotidiens gratuits, spins de roue) **sans avoir à visionner la moindre publicité**.

---

### 📲 Comment utiliser ces patchs dans Morphe Manager

Cliquez sur le lien ci-dessous pour ajouter directement cette source de patchs à Morphe Manager :

👉 **[Ajouter D-moniak Patches à Morphe Manager](https://morphe.software/add-source?github=SatanMerde/D-moniakPatches)**

Ou manuellement dans **Morphe Manager** :
1. Ouvrez Morphe Manager.
2. Rendez-vous dans les paramètres des **Sources de patchs** (*Patch Sources*).
3. Ajoutez la source GitHub : `SatanMerde/D-moniakPatches`.
4. Sélectionnez l'application **Hungry Shark World** (`com.ubisoft.hungrysharkworld`).
5. Sélectionnez le patch **Bypass Rewarded Ads**.
6. Cliquez sur **Patcher**, puis installez l'APK généré.

---

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->

### 🦈 Hungry Shark World (`com.ubisoft.hungrysharkworld`)

| Nom du Patch | Description | Activé par défaut |
| :--- | :--- | :---: |
| **Bypass Rewarded Ads** | Permet de récupérer toutes les récompenses associées aux pubs (réanimations, pièces, gemmes, coffres) instantanément sans visionner de publicité. | ✅ |

<!-- PATCHES_END -->

---

## 🛠️ Développement & Publication

Ce dépôt utilise le système de publication automatisé **Semantic Release** avec GitHub Actions :

- **Branche `dev`** : Utilisée pour le développement et la création automatique de pré-releases.
- **Branche `main`** : Branche stable de production.
- **Types de commits conventionnels** :
  - `feat:` Déclenche une nouvelle version mineure avec release automatique.
  - `fix:` Déclenche une version de correction (patch release).
  - `chore:` Maintenance sans déclencher de publication publique.

### Compilation locale

```bash
# Compiler le bundle de patchs (.mpp)
./gradlew :patches:buildAndroid
```
Le fichier `.mpp` résultant se trouve dans `patches/build/libs/patches-*.mpp` et peut être utilisé directement avec [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop).

> [!NOTE]
> Pour compiler localement avec Gradle, vous devez configurer un GitHub Personal Access Token (PAT) avec le scope `read:packages` dans votre fichier `~/.gradle/gradle.properties` (`gpr.user` et `gpr.key`).
> Lors de l'exécution sur GitHub Actions (`release.yml`), le token est fourni automatiquement.

## 📄 Licence

Ce projet est sous licence [GNU General Public License v3.0](LICENSE).
