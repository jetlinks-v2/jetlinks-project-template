export type PermissionScope = {
    id: string
    actions: string[]
}

export type PermissionDefinition = {
    id: string
    name: string
    i18nName?: string
    actions?: { action: string; name: string; i18nName?: string }[]
}

// 将可授权定义与当前范围合并，标签按范围展示，查看时仍反映原令牌的授权。
export const handleData = (_arr: PermissionDefinition[], checkedValue: PermissionScope[]) => {
    return _arr.map((item) => {
        const checked = checkedValue?.find(
            (checkedItem) => checkedItem.id === item.id,
        )

        const options =
            (item.actions &&
                item.actions.map((actionItem) => ({
                    label: actionItem.i18nName || actionItem.name,
                    value: actionItem.action,
                }))) ||
            []
        const checkedList = checked?.actions || []
        return {
            id: item.id,
            name: item.i18nName ||item.name,
            checkedList,
            checkedOptions: options.filter((option) => checkedList.includes(option.value)),
            checkAll:
                (checked &&
                    item.actions &&
                    checked.actions.length === item.actions.length) ||
                false,
            indeterminate:
                (checked &&
                    item.actions &&
                    checked.actions.length < item.actions.length) ||
                false,
            options,
        }
    })
}
