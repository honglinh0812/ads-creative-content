<template>
  <CreativeErrorPage
    :error-code="404"
    error-type="not-found"
    :show-home-button="true"
    :show-back-button="true"
    :show-refresh-button="false"
  />
</template>

<script>
import CreativeErrorPage from '@/components/ui/CreativeErrorPage.vue'

export default {
  name: 'NotFound',
  components: {
    CreativeErrorPage
  }
}
</script>
