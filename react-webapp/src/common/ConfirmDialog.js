import confirm from 'reactstrap-confirm';

export function confirmDialog(titleDialog, bodyMessage, labelMessage) {
    return confirm({
        title: titleDialog,
        message: bodyMessage,
        confirmText: labelMessage,
        confirmColor: "primary",
        cancelColor: "link text-danger"
    });
}