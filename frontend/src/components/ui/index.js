// UI Kit Components
import Button from './Button.vue'
import Input from './Input.vue'
import Card from './Card.vue'
import Modal from './Modal.vue'

// Export individual components
export {
  Button,
  Input,
  Card,
  Modal
}

// Export as default object for easier importing
export default {
  Button,
  Input,
  Card,
  Modal
}

// Plugin for global registration
export const UIKitPlugin = {
  install(app) {
    app.component('UiButton', Button)
    app.component('UiInput', Input)
    app.component('UiCard', Card)
    app.component('UiModal', Modal)
  }
}