import {useInstanceStore} from "@device-manager-ui/store/instance";
import type {MetadataType} from "@device-manager-ui/views/Product/typings";

const limitsMap = new Map<string, any>();
limitsMap.set('events-add', 'eventNotInsertable');
limitsMap.set('events-update', 'eventNotModifiable');
limitsMap.set('properties-add', 'propertyNotInsertable');
limitsMap.set('properties-update', 'propertyNotModifiable');

const useOperateLimits = (target: 'device' | 'product' | 'template') => {

    const instanceStore = useInstanceStore()

    const operates = computed(() => {
        return target === 'device' ? (instanceStore.current?.features || []) : []
    })

    const hasOperate = (action: 'add' | 'update', types: MetadataType ) => {
        return !!operates.value.find((item: { id: string; name: string }) => item.id === limitsMap.get(`${types}-${action}`))
    }

    return {
        hasOperate
    }
}

export default useOperateLimits
