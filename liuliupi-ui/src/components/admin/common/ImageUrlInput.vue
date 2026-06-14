<template>
  <div class="image-url-input">
    <el-input
      :value="value"
      @input="$emit('input', $event)"
      :placeholder="placeholder"
      class="image-url-input-field"
    />
    <el-image
      v-if="value"
      :src="value"
      :preview-src-list="[value]"
      fit="cover"
      class="image-url-input-thumb"
    />
    <el-button
      type="primary"
      plain
      size="small"
      @click="dialogVisible = true"
      class="image-url-input-btn"
    >
      上传
    </el-button>

    <el-dialog
      :title="label + '上传'"
      :visible.sync="dialogVisible"
      width="420px"
      append-to-body
    >
      <upload-picture
        v-bind="$attrs"
        @addPicture="handleUploadSuccess"
      />
    </el-dialog>
  </div>
</template>

<script>
  const uploadPicture = () => import('@/components/common/uploadPicture')

  export default {
    name: 'ImageUrlInput',
    components: { uploadPicture },
    inheritAttrs: false,
    props: {
      value: {
        type: String,
        default: ''
      },
      label: {
        type: String,
        default: '图片'
      },
      placeholder: {
        type: String,
        default: 'https://'
      }
    },
    data() {
      return {
        dialogVisible: false
      }
    },
    methods: {
      handleUploadSuccess(url) {
        this.$emit('input', url)
        this.dialogVisible = false
      }
    }
  }
</script>

<style scoped>
  .image-url-input {
    display: flex;
    align-items: center;
    gap: 10px;
    flex: 1;
  }

  .image-url-input-field {
    flex: 1;
    max-width: 420px;
  }

  .image-url-input-thumb {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    border: 1px solid #e4e7ed;
    flex-shrink: 0;
  }

  .image-url-input-btn {
    flex-shrink: 0;
  }
</style>
