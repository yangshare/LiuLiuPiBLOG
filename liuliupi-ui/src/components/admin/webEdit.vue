<template>
  <div>
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="基础信息" name="basic">
        <el-form :model="webInfo" :rules="rules" ref="ruleForm" label-width="100px"
                 class="demo-ruleForm">
          <el-form-item label="网站名称" prop="webName">
            <el-input v-model="webInfo.webName"></el-input>
          </el-form-item>

          <el-form-item label="网站标题" prop="webTitle">
            <el-input v-model="webInfo.webTitle"></el-input>
          </el-form-item>

          <el-form-item label="页脚" prop="footer">
            <el-input v-model="webInfo.footer"></el-input>
          </el-form-item>

          <el-form-item label="状态" prop="status">
            <el-switch @click.native="changeWebStatus(webInfo)" v-model="webInfo.status"></el-switch>
          </el-form-item>

          <el-form-item label="背景" prop="backgroundImage">
            <ImageUrlInput
              v-model="webInfo.backgroundImage"
              label="背景"
              prefix="webBackgroundImage"
              :maxSize="3"
              :maxNumber="1"
            />
          </el-form-item>

          <el-form-item label="头像" prop="avatar">
            <ImageUrlInput
              v-model="webInfo.avatar"
              label="头像"
              prefix="webAvatar"
              :maxSize="2"
              :maxNumber="1"
            />
          </el-form-item>

          <el-form-item label="提示" prop="waifuJson">
            <div style="display: flex">
              <el-input :disabled="disabled" :rows="6" type="textarea" v-model="webInfo.waifuJson"></el-input>
              <i class="el-icon-edit my-icon" @click="disabled = !disabled"></i>
            </div>
          </el-form-item>
        </el-form>
        <div class="form-actions">
          <el-button type="primary" @click="submitForm('ruleForm')">保存</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="公告" name="notice">
        <div class="tag-block">
          <el-tag
            :key="i"
            v-for="(notice, i) in notices"
            closable
            :disable-transitions="false"
            @close="handleClose(notices, notice)">
            {{notice}}
          </el-tag>
          <el-input
            class="input-new-tag"
            v-if="inputNoticeVisible"
            v-model="inputNoticeValue"
            ref="saveNoticeInput"
            size="small"
            @keyup.enter.native="handleInputNoticeConfirm"
            @blur="handleInputNoticeConfirm">
          </el-input>
          <el-button v-else class="button-new-tag" size="small" @click="showNoticeInput()">+ 公告</el-button>
        </div>
        <div class="form-actions">
          <el-button type="primary" @click="saveNotice()">保存</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="随机资源" name="random">
        <el-card class="random-resource-card">
          <div slot="header">随机名称</div>
          <div class="tag-block">
            <el-tag
              :key="i"
              effect="dark"
              v-for="(name, i) in randomName"
              closable
              :disable-transitions="false"
              :type="types[Math.floor(Math.random() * 5)]"
              @close="handleClose(randomName, name)">
              {{name}}
            </el-tag>
            <el-input
              class="input-new-tag"
              v-if="inputRandomNameVisible"
              v-model="inputRandomNameValue"
              ref="saveRandomNameInput"
              size="small"
              @keyup.enter.native="handleInputRandomNameConfirm"
              @blur="handleInputRandomNameConfirm">
            </el-input>
            <el-button v-else class="button-new-tag" size="small" @click="showRandomNameInput">+ 随机名称</el-button>
          </div>
        </el-card>

        <el-card class="random-resource-card">
          <div slot="header">随机头像</div>
          <div class="random-image-grid">
            <div :key="i"
                 class="random-image-item"
                 v-for="(avatar, i) in randomAvatar">
              <el-image lazy class="random-image-thumb"
                        :preview-src-list="[avatar]"
                        :src="avatar"
                        fit="cover"></el-image>
              <el-tag
                class="random-image-url"
                closable
                :disable-transitions="false"
                @close="handleClose(randomAvatar, avatar)">
                {{avatar}}
              </el-tag>
            </div>
            <div v-if="randomAvatar.length === 0" class="empty-tip">暂无图片，点击下方按钮添加。</div>
          </div>

          <el-button
            class="button-new-tag"
            size="small"
            type="primary"
            plain
            icon="el-icon-link"
            @click="showAddUrlDialog('avatar')"
          >
            添加图片链接
          </el-button>
          <div class="upload-divider">或上传本地图片（一次最多 5 张，每张不超过 1M）</div>
          <uploadPicture prefix="randomAvatar" style="margin: 10px" @addPicture="addRandomAvatar"
                         :maxSize="1"
                         :maxNumber="5"></uploadPicture>
        </el-card>

        <el-card class="random-resource-card">
          <div slot="header">随机封面</div>
          <div class="random-image-grid">
            <div :key="i"
                 class="random-image-item"
                 v-for="(cover, i) in randomCover">
              <el-image lazy class="random-image-thumb"
                        :preview-src-list="[cover]"
                        :src="cover"
                        fit="cover"></el-image>
              <el-tag
                class="random-image-url"
                closable
                :disable-transitions="false"
                @close="handleClose(randomCover, cover)">
                {{cover}}
              </el-tag>
            </div>
            <div v-if="randomCover.length === 0" class="empty-tip">暂无图片，点击下方按钮添加。</div>
          </div>

          <el-button
            class="button-new-tag"
            size="small"
            type="primary"
            plain
            icon="el-icon-link"
            @click="showAddUrlDialog('cover')"
          >
            添加图片链接
          </el-button>
          <div class="upload-divider">或上传本地图片（一次最多 5 张，每张不超过 1M）</div>
          <uploadPicture prefix="randomCover" style="margin: 10px" @addPicture="addRandomCover"
                         :maxSize="2"
                         :maxNumber="5"></uploadPicture>
        </el-card>

        <div class="form-actions">
          <el-button type="primary" @click="saveRandomResources()">保存</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      title="添加图片链接"
      :visible.sync="addUrlDialogVisible"
      width="420px"
      append-to-body
    >
      <el-form>
        <el-form-item :error="addUrlError">
          <el-input
            v-model="addUrlValue"
            placeholder="https://example.com/avatar.png"
            size="small"
          />
        </el-form-item>
      </el-form>
      <div class="dialog-tip">支持 jpg、png、gif、webp、svg 等常见图片格式。</div>
      <span slot="footer">
        <el-button size="small" @click="addUrlDialogVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="confirmAddUrl">确认添加</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
  const uploadPicture = () => import("../common/uploadPicture");
  import ImageUrlInput from "./common/ImageUrlInput";

  export default {
    components: {
      uploadPicture,
      ImageUrlInput
    },
    data() {
      return {
        activeTab: 'basic',
        disabled: true,
        types: ['', 'success', 'info', 'danger', 'warning'],
        inputNoticeVisible: false,
        inputNoticeValue: "",
        inputRandomNameVisible: false,
        inputRandomNameValue: "",
        addUrlType: '', // 'avatar' | 'cover'
        addUrlDialogVisible: false,
        addUrlValue: '',
        addUrlError: '',
        webInfo: {
          id: null,
          webName: "",
          webTitle: "",
          footer: "",
          backgroundImage: "",
          avatar: "",
          waifuJson: "",
          status: false
        },
        notices: [],
        randomAvatar: [],
        randomName: [],
        randomCover: [],
        rules: {
          webName: [
            {required: true, message: '请输入网站名称', trigger: 'blur'},
            {min: 1, max: 10, message: '长度在 1 到 10 个字符', trigger: 'change'}
          ],
          webTitle: [
            {required: true, message: '请输入网站标题', trigger: 'blur'}
          ],
          footer: [
            {required: true, message: '请输入页脚', trigger: 'blur'}
          ],
          backgroundImage: [
            {required: true, message: '请输入背景', trigger: 'change'}
          ],
          status: [
            {required: true, message: '请设置网站状态', trigger: 'change'}
          ],
          avatar: [
            {required: true, message: '请上传头像', trigger: 'change'}
          ]
        }
      }
    },

    computed: {},

    watch: {},

    created() {
      this.getWebInfo();
    },

    mounted() {

    },

    methods: {
      addRandomAvatar(res) {
        this.randomAvatar.push(res);
      },
      addRandomCover(res) {
        this.randomCover.push(res);
      },
      showAddUrlDialog(type) {
        this.addUrlType = type
        this.addUrlDialogVisible = true
      },
      validateImageUrl(url) {
        if (!url) {
          return '请输入有效的图片链接'
        }
        if (!/^https?:\/\//i.test(url)) {
          return '请输入有效的图片链接'
        }
        if (!/\.(jpg|jpeg|png|gif|webp|svg)$/i.test(url)) {
          return '请输入有效的图片链接'
        }
        return ''
      },
      confirmAddUrl() {
        const error = this.validateImageUrl(this.addUrlValue)
        if (error) {
          this.addUrlError = error
          return
        }
        if (this.addUrlType === 'avatar') {
          this.randomAvatar.push(this.addUrlValue)
        } else if (this.addUrlType === 'cover') {
          this.randomCover.push(this.addUrlValue)
        }
        this.addUrlDialogVisible = false
        this.addUrlValue = ''
        this.addUrlError = ''
      },
      changeWebStatus(webInfo) {
        this.$http.post(this.$constant.baseURL + "/webInfo/updateWebInfo", {
          id: webInfo.id,
          status: webInfo.status
        }, true)
          .then((res) => {
            this.getWebInfo();
            this.$message({
              message: "保存成功！",
              type: "success"
            });
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
          });
      },
      getWebInfo() {
        this.$http.get(this.$constant.baseURL + "/admin/webInfo/getAdminWebInfo", {}, true)
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.webInfo.id = res.data.id;
              this.webInfo.webName = res.data.webName;
              this.webInfo.webTitle = res.data.webTitle;
              this.webInfo.footer = res.data.footer;
              this.webInfo.backgroundImage = res.data.backgroundImage;
              this.webInfo.avatar = res.data.avatar;
              this.webInfo.waifuJson = res.data.waifuJson;
              this.webInfo.status = res.data.status;
              this.notices = JSON.parse(res.data.notices);
              this.randomAvatar = JSON.parse(res.data.randomAvatar);
              this.randomName = JSON.parse(res.data.randomName);
              this.randomCover = JSON.parse(res.data.randomCover);
            }
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
          });
      },
      submitForm(formName) {
        this.$refs[formName].validate((valid) => {
          if (valid) {
            this.updateWebInfo(this.webInfo)
          } else {
            this.$message({
              message: "请完善必填项！",
              type: "error"
            });
          }
        });
      },
      handleClose(array, item) {
        array.splice(array.indexOf(item), 1);
      },
      handleInputNoticeConfirm() {
        if (this.inputNoticeValue) {
          this.notices.push(this.inputNoticeValue);
        }
        this.inputNoticeVisible = false;
        this.inputNoticeValue = '';
      },
      showNoticeInput() {
        this.inputNoticeVisible = true;
        this.$nextTick(() => {
          this.$refs.saveNoticeInput.$refs.input.focus();
        });
      },
      saveNotice() {
        let param = {
          id: this.webInfo.id,
          notices: JSON.stringify(this.notices)
        }
        this.updateWebInfo(param);
      },
      handleInputRandomNameConfirm() {
        if (this.inputRandomNameValue) {
          this.randomName.push(this.inputRandomNameValue);
        }
        this.inputRandomNameVisible = false;
        this.inputRandomNameValue = '';
      },
      showRandomNameInput() {
        this.inputRandomNameVisible = true;
        this.$nextTick(() => {
          this.$refs.saveRandomNameInput.$refs.input.focus();
        });
      },
      saveRandomResources() {
        this.updateWebInfo({
          id: this.webInfo.id,
          randomName: JSON.stringify(this.randomName),
          randomAvatar: JSON.stringify(this.randomAvatar),
          randomCover: JSON.stringify(this.randomCover)
        })
      },
      updateWebInfo(value) {
        this.$confirm('确认保存？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success',
          center: true
        }).then(() => {
          this.$http.post(this.$constant.baseURL + "/webInfo/updateWebInfo", value, true)
            .then((res) => {
              this.getWebInfo();
              this.$message({
                message: "保存成功！",
                type: "success"
              });
            })
            .catch((error) => {
              this.$message({
                message: error.message,
                type: "error"
              });
            });
        }).catch(() => {
          this.$message({
            type: 'success',
            message: '已取消保存!'
          });
        });
      }
    }
  }
</script>

<style scoped>

  .form-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 24px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
  }

  .tag-block {
    margin-top: 10px;
  }

  .random-resource-card {
    margin-bottom: 20px;
  }

  .random-resource-card:last-of-type {
    margin-bottom: 0;
  }

  .random-image-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 12px;
    margin: 10px;
  }

  .random-image-item {
    min-width: 0;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    padding: 10px;
    background: #ffffff;
  }

  .random-image-thumb {
    width: 100%;
    height: 120px;
    border-radius: 2px;
    border: 1px solid #e4e7ed;
  }

  .random-image-url {
    display: flex;
    align-items: center;
    margin: 10px 0 0;
    white-space: normal;
    height: auto;
    word-break: break-all;
  }

  .el-tag {
    margin: 10px;
  }

  .button-new-tag {
    margin: 10px;
    height: 32px;
    line-height: 32px;
    padding-top: 0;
    padding-bottom: 0;
  }

  .input-new-tag {
    width: 200px;
    margin: 10px;
  }

  .my-icon {
    cursor: pointer;
    margin-left: 10px;
    font-size: 18px;
    font-weight: bold;
    color: var(--blue);
  }

  .table-td-thumb {
    border-radius: 2px;
    width: 40px;
    height: 40px;
  }

  .upload-divider {
    border-top: 1px dashed #dcdfe6;
    padding-top: 16px;
    margin-top: 16px;
    margin-bottom: 10px;
    font-size: 12px;
    color: #909399;
  }

  .dialog-tip {
    font-size: 12px;
    color: #909399;
    margin-top: -8px;
    line-height: 1.5;
  }

  .empty-tip {
    grid-column: 1 / -1;
    font-size: 14px;
    color: #909399;
    text-align: center;
    padding: 20px 0;
  }

</style>
