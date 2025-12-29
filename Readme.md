# SimplePoint
**Version:** 1.0.0<br>
**Native Minecraft Version:** 1.16.5<br>
**Author:** pino223<br>
**LICENSE:** [GPL-3.0](LICENSE)<br>

## 概要
シンプルだけど使いやすいを目指す複数ポイント対応のポイント管理プラグイン。  

## コマンド
`/spp <subcommand>` 管理者用のコマンド  
`/spt <subcommand>` 鯖民向けのコマンド

## サブコマンド
### spp
* `create <新しく作りたいpoint名>`  
  新しくポイントを作ります。日本語可。すべてはここから始まる。
* `add <player> <point> <number>`  
  指定したプレイヤーのポイントを増やします。累計獲得に含まれます。
* `remove <player> <point> <number>`  
  指定したプレイヤーのポイントを減らします。
* `set <player> <point> <number>`  
  指定したプレイヤーのポイントを設定します。累計獲得に反映されません。
* `score <point> <player>`  
  指定したプレイヤーのポイントを閲覧します。
* `rewardgui <point>`  
  指定したポイントの報酬を編集できるGUIを開きます。
* `setreq <point> <slot> <number>`  
  指定したスロットの報酬に、開放に必要な累計ポイント数の制限をかけます。
* `ranking <point>`  
  指定したポイントのランキング（上位10名）を**全体チャット**に表示します。盛り上げたい時に！
* `toggleranking <point>`  
  指定したポイントのランキング表示・報酬ショップを有効/無効にします。
* `reload`  
  configや報酬、ポイントデータのキャッシュをリロードします。
* `help`  
  helpを表示します。

### spt
* `myp <point>`  
  自分の現在の所持ポイントとこれまでの累計獲得ポイントを確認します。
* `reward <point>`  
  報酬ショップGUIを開きます。
* `ranking <point>`  
  上位7名のランキングと、**自分の現在の順位**を確認します。

***

## 機能

### 報酬設定GUI
`/spp rewardgui` で開いた画面にアイテムを置くだけで設定開始！
* **在庫設定**: 有限在庫だけでなく、コンパレーターのボタンで「無限在庫」に切り替え可能。
* **装飾モード**: 絵画のボタンで「装飾モード」に切り替えると、アイテム名と説明文だけが表示され、価格や在庫などの要素が表示されなくなります（購入も不可）。
* **コマンド実行**: `rewards/ポイント名.yml` を直接編集することで、購入時にコンソールからコマンド（`%player%` `%point%`）を実行できます。
```
commands:
    - "broadcast %point% から %player% が豪華賞品をゲットしました！"
    - "give %player% diamond 1"
```
    
### ログ記録
`logs.csv` に記録します。

***
teamに関するコマンドがありますが未実装です！
