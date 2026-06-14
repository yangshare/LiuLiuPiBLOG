<template>
  <div class="myCenter verify-container">
    <div class="verify-content">
      <div>
        <el-avatar :size="50" :src="$store.state.webInfo.avatar"></el-avatar>
      </div>
      <div>
        <el-input v-model="account">
          <template slot="prepend">账号</template>
        </el-input>
      </div>
      <div>
        <el-input v-model="password" type="password">
          <template slot="prepend">密码</template>
        </el-input>
      </div>
      <div class="captcha-row">
        <el-input v-model="captchaCode" placeholder="验证码" class="captcha-input"></el-input>
        <img :src="captchaImage" @click="getCaptcha()"
             class="captcha-img" alt="验证码" title="点击刷新">
      </div>
      <div>
        <proButton :info="'提交'"
                   @click.native="login()"
                   :before="$constant.before_color_2"
                   :after="$constant.after_color_2">
        </proButton>
      </div>
    </div>
  </div>
</template>

<script>
  const proButton = () => import( "../common/proButton");

  export default {
    components: {
      proButton
    },
    data() {
      return {
        redirect: this.$route.query.redirect,
        account: "",
        password: "",
        captchaToken: "",
        captchaImage: "",
        captchaCode: ""
      }
    },
    computed: {},
    created() {
      let sysConfig = this.$store.state.sysConfig;
      if (!this.$common.isEmpty(sysConfig) && !this.$common.isEmpty(sysConfig['webStaticResourcePrefix'])) {
        let root = document.querySelector(":root");
        let webStaticResourcePrefix = sysConfig['webStaticResourcePrefix'];
        root.style.setProperty("--backgroundPicture", "url(" + webStaticResourcePrefix + "assets/backgroundPicture.jpg)");
      }
      this.getCaptcha();
    },
    methods: {
      getCaptcha() {
        this.$http.get(this.$constant.baseURL + "/user/captcha")
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.captchaToken = res.data.captchaToken;
              this.captchaImage = res.data.image;
            }
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
          });
      },
      login() {
        if (this.$common.isEmpty(this.account) || this.$common.isEmpty(this.password)) {
          this.$message({
            message: "请输入账号或密码！",
            type: "error"
          });
          return;
        }

        if (this.$common.isEmpty(this.captchaCode)) {
          this.$message({
            message: "请输入验证码！",
            type: "error"
          });
          return;
        }

        let user = {
          account: this.account.trim(),
          password: this.$common.encrypt(this.password.trim()),
          captchaToken: this.captchaToken,
          code: this.captchaCode.trim()
        };

        this.$http.post(this.$constant.baseURL + "/user/login", user, false)
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              // 统一 Token 体系：使用 currentUser 和 userToken
              localStorage.setItem("userToken", res.data.accessToken);
              this.$store.commit("loadCurrentUser", res.data);
              this.account = "";
              this.password = "";
              this.captchaCode = "";
              this.$router.push({path: this.redirect});
            }
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
            // 验证码已被一次性消费，刷新图片并清空输入
            this.captchaCode = "";
            this.getCaptcha();
          });
      }
    }
  }
</script>

<style scoped>

  .verify-container {
    height: 100vh;
    background: var(--backgroundPicture) center center / cover repeat;
  }

  .verify-content {
    background: var(--maxWhiteMask);
    padding: 30px 40px 5px;
    position: relative;
  }

  .verify-content > div:first-child {
    position: absolute;
    left: 50%;
    transform: translate(-50%);
    top: -25px;
  }

  .verify-content > div:not(:first-child) {
    margin: 25px 0;
  }

  .verify-content > div:last-child > div {
    margin: 0 auto;
  }

  .captcha-row {
    display: flex;
    align-items: center;
  }

  .captcha-input {
    flex: 1;
  }

  .captcha-img {
    width: 120px;
    height: 40px;
    margin-left: 10px;
    cursor: pointer;
    border-radius: 4px;
  }

</style>
